/*
 * Copyright 2018 The RoboZonky Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robozonky.app.daemon.operations;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.ws.rs.BadRequestException;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.PurchaseRecommendedEvent;
import com.github.robozonky.api.notifications.PurchaseRequestedEvent;
import com.github.robozonky.api.notifications.PurchasingCompletedEvent;
import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.daemon.BlockedAmountProcessor;
import com.github.robozonky.app.daemon.Portfolio;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchasingTest extends AbstractZonkyLeveragingTest {

    private static final PurchaseStrategy NONE_ACCEPTING_STRATEGY = (a, p, r) -> Stream.empty(),
            ALL_ACCEPTING_STRATEGY = (a, p, r) -> a.stream().map(d -> d.recommend().get());
    private static final Supplier<Optional<PurchaseStrategy>> ALL_ACCEPTING = () -> Optional.of(ALL_ACCEPTING_STRATEGY),
            NONE_ACCEPTING = () -> Optional.of(NONE_ACCEPTING_STRATEGY);

    @Test
    void noStrategy() {
        final Participation mock = mock(Participation.class);
        final Tenant tenant = mockTenant();
        final Purchasing exec = new Purchasing(Optional::empty, tenant);
        final Portfolio portfolio = mock(Portfolio.class);
        assertThat(exec.apply(portfolio, Collections.singleton(mock))).isEmpty();
        // check events
        final List<Event> events = getEventsRequested();
        assertThat(events).isEmpty();
    }

    @Test
    void noneAccepted() {
        final Zonky zonky = harmlessZonky(9_000);
        when(zonky.getLoan(anyInt()))
                .thenAnswer(invocation -> {
                    final int id = invocation.getArgument(0);
                    return Loan.custom().setId(id).setAmount(200).build();
                });
        final Participation mock = mock(Participation.class);
        when(mock.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(250));
        final Tenant auth = mockTenant(zonky);
        final Purchasing exec = new Purchasing(NONE_ACCEPTING, auth);
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        assertThat(exec.apply(portfolio, Collections.singleton(mock))).isEmpty();
        final List<Event> e = getEventsRequested();
        assertThat(e).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(e).first().isInstanceOf(PurchasingStartedEvent.class);
            softly.assertThat(e).last().isInstanceOf(PurchasingCompletedEvent.class);
        });
    }

    @Test
    void someAccepted() {
        final int loanId = 1;
        final Loan loan = Loan.custom()
                .setId(loanId)
                .setAmount(100_000)
                .setRating(Rating.D)
                .setRemainingInvestment(1000)
                .setMyInvestment(mockMyInvestment())
                .setDatePublished(OffsetDateTime.now())
                .build();
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getLoan(eq(loanId))).thenReturn(loan);
        final Participation mock = mock(Participation.class);
        when(mock.getId()).thenReturn(1L);
        when(mock.getLoanId()).thenReturn(loan.getId());
        when(mock.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(250));
        when(mock.getRating()).thenReturn(loan.getRating());
        final Tenant auth = mockTenant(zonky);
        final Purchasing exec = new Purchasing(ALL_ACCEPTING, auth);
        final Portfolio portfolio = spy(Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth)));
        assertThat(exec.apply(portfolio, Collections.singleton(mock))).isNotEmpty();
        verify(zonky, never()).purchase(eq(mock)); // do not purchase as we're in dry run
        verify(portfolio).simulateCharge(loanId, loan.getRating(), mock.getRemainingPrincipal());
        final List<Event> e = getEventsRequested();
        assertThat(e).hasSize(5);
        assertSoftly(softly -> {
            softly.assertThat(e).first().isInstanceOf(PurchasingStartedEvent.class);
            softly.assertThat(e.get(1)).isInstanceOf(PurchaseRecommendedEvent.class);
            softly.assertThat(e.get(2)).isInstanceOf(PurchaseRequestedEvent.class);
            softly.assertThat(e.get(3)).isInstanceOf(InvestmentPurchasedEvent.class);
            softly.assertThat(e).last().isInstanceOf(PurchasingCompletedEvent.class);
        });
        // doing a dry run; the same participation is now ignored
        assertThat(exec.apply(portfolio, Collections.singleton(mock))).isEmpty();
    }

    @Test
    void tryPurchaseButZonkyFail() {
        final int loanId = 1;
        final Loan loan = Loan.custom()
                .setId(loanId)
                .setAmount(100_000)
                .setRating(Rating.D)
                .setRemainingInvestment(1000)
                .setMyInvestment(mockMyInvestment())
                .setDatePublished(OffsetDateTime.now())
                .build();
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getLoan(eq(loanId))).thenReturn(loan);
        doThrow(BadRequestException.class).when(zonky).purchase(any());
        final Participation mock = mock(Participation.class);
        when(mock.getId()).thenReturn(1L);
        when(mock.getLoanId()).thenReturn(loan.getId());
        when(mock.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(250));
        when(mock.getRating()).thenReturn(loan.getRating());
        final Tenant auth = mockTenant(zonky, false);
        final Purchasing exec = new Purchasing(ALL_ACCEPTING, auth);
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        assertThatThrownBy(() -> exec.apply(portfolio, Collections.singleton(mock)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void noItems() {
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant auth = mockTenant(zonky);
        final Purchasing exec = new Purchasing(ALL_ACCEPTING, auth);
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        assertThat(exec.apply(portfolio, Collections.emptyList())).isEmpty();
        final List<Event> e = getEventsRequested();
        assertThat(e).isEmpty();
    }
}
