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
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.EventTenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchasingTest extends AbstractZonkyLeveragingTest {

    private static final PurchaseStrategy NONE_ACCEPTING_STRATEGY = (a, p, r) -> Stream.empty(),
            ALL_ACCEPTING_STRATEGY = (a, p, r) -> a.stream().map(d -> d.recommend().get());

    @Test
    void noStrategy() {
        final Participation mock = mock(Participation.class);
        final ParticipationDescriptor pd = new ParticipationDescriptor(mock, () -> Loan.custom().build());
        final EventTenant tenant = mockTenant();
        final Purchasing exec = new Purchasing(tenant);
        assertThat(exec.apply(Collections.singleton(pd))).isEmpty();
        // check events
        final List<Event> events = getEventsRequested();
        assertThat(events).isEmpty();
    }

    @Test
    void noneAccepted() {
        final Zonky zonky = harmlessZonky(9_000);
        final Loan loan = Loan.custom().setId(1).setAmount(200).build();
        when(zonky.getLoan(eq(loan.getId()))).thenReturn(loan);
        final Participation mock = mock(Participation.class);
        when(mock.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(250));
        final ParticipationDescriptor pd = new ParticipationDescriptor(mock, () -> loan);
        final EventTenant auth = mockTenant(zonky);
        when(auth.getPurchaseStrategy()).thenReturn(Optional.of(NONE_ACCEPTING_STRATEGY));
        final Purchasing exec = new Purchasing(auth);
        assertThat(exec.apply(Collections.singleton(pd))).isEmpty();
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
        final ParticipationDescriptor pd = new ParticipationDescriptor(mock, () -> loan);
        final EventTenant auth = mockTenant(zonky);
        when(auth.getPurchaseStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        final Purchasing exec = new Purchasing(auth);
        assertThat(exec.apply(Collections.singleton(pd))).isNotEmpty();
        verify(zonky, never()).purchase(eq(mock)); // do not purchase as we're in dry run
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
        assertThat(exec.apply(Collections.singleton(pd))).isEmpty();
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
        final ParticipationDescriptor pd = new ParticipationDescriptor(mock, () -> loan);
        final EventTenant auth = mockTenant(zonky, false);
        when(auth.getPurchaseStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        final Purchasing exec = new Purchasing(auth);
        assertThat(exec.apply(Collections.singleton(pd))).isEmpty();
    }

    @Test
    void noItems() {
        final Zonky zonky = harmlessZonky(10_000);
        final EventTenant auth = mockTenant(zonky);
        final Purchasing exec = new Purchasing(auth);
        when(auth.getPurchaseStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        assertThat(exec.apply(Collections.emptyList())).isEmpty();
        final List<Event> e = getEventsRequested();
        assertThat(e).isEmpty();
    }
}
