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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.daemon.BlockedAmountProcessor;
import com.github.robozonky.app.daemon.Portfolio;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvestingTest extends AbstractZonkyLeveragingTest {

    private static final InvestmentStrategy NONE_ACCEPTING_STRATEGY = (a, p, r) -> Stream.empty(),
            ALL_ACCEPTING_STRATEGY = (a, p, r) -> a.stream().map(d -> d.recommend(200).get());
    private static final Supplier<Optional<InvestmentStrategy>> NONE_ACCEPTING =
            () -> Optional.of(NONE_ACCEPTING_STRATEGY),
            ALL_ACCEPTING = () -> Optional.of(ALL_ACCEPTING_STRATEGY);

    @Test
    void noStrategy() {
        final int loanId = (int) (Math.random() * 1000); // new ID every time to avoid caches
        final Loan loan = Loan.custom()
                .setId(loanId)
                .setAmount(2)
                .build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Investing exec = new Investing(null, Optional::empty, null);
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(1000);
        final Tenant auth = mockTenant(z);
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        assertThat(exec.apply(portfolio, Collections.singletonList(ld))).isEmpty();
        // check events
        final List<Event> events = getEventsRequested();
        assertThat(events).isEmpty();
    }

    @Test
    void noItems() {
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(1000);
        final Tenant auth = mockTenant(z);
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        final Investor builder = Investor.build(auth);
        final Investing exec = new Investing(builder, ALL_ACCEPTING, auth);
        assertThat(exec.apply(portfolio, Collections.emptyList())).isEmpty();
    }

    @Test
    void noneAccepted() {
        final int loanId = (int) (Math.random() * 1000); // new ID every time to avoid caches
        final Loan loan = Loan.custom()
                .setId(loanId)
                .setAmount(100_000)
                .setRating(Rating.D)
                .setMyInvestment(mockMyInvestment())
                .setDatePublished(OffsetDateTime.now())
                .build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Zonky z = harmlessZonky(9000);
        final Tenant auth = mockTenant(z);
        final Investor builder = Investor.build(auth);
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        when(z.getLoan(eq(loanId))).thenReturn(loan);
        final Investing exec = new Investing(builder, NONE_ACCEPTING, auth);
        assertThat(exec.apply(portfolio, Collections.singleton(ld))).isEmpty();
    }

    @Test
    void rejected() {
        final Loan loan = Loan.custom()
                .setAmount(100_000)
                .setRating(Rating.D)
                .setRemainingInvestment(100_000)
                .setMyInvestment(mockMyInvestment())
                .setDatePublished(OffsetDateTime.now())
                .build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Zonky z = harmlessZonky(9000);
        final Tenant auth = mockTenant(z);
        final Investor investor = mock(Investor.class);
        when(investor.getConfirmationProvider()).thenReturn(Optional.of(mock(ConfirmationProvider.class)));
        when(investor.invest(any(), anyBoolean())).thenReturn(new ZonkyResponse(ZonkyResponseType.REJECTED));
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        when(z.getLoan(eq(loan.getId()))).thenReturn(loan);
        final Investing exec = new Investing(investor, ALL_ACCEPTING, auth);
        assertThat(exec.apply(portfolio, Collections.singleton(ld))).isEmpty();
        verify(investor, times(1)).invest(any(), anyBoolean()); // call to invest
        verify(z, never()).invest(any()); // does not propagate to Zonky
        // now try again, the same loan should now be discarded and therefore not even attempted
        final Investing exec2 = new Investing(investor, ALL_ACCEPTING, auth);
        assertThat(exec2.apply(portfolio, Collections.singleton(ld))).isEmpty();
        verify(investor, times(1)).invest(any(), anyBoolean()); // call to invest
    }

    @Test
    void someAccepted() {
        final Loan loan = Loan.custom()
                .setAmount(100_000)
                .setRating(Rating.C)
                .setRemainingInvestment(20_000)
                .build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Zonky z = harmlessZonky(10_000);
        final int loanId = loan.getId(); // will be random, to avoid problems with caching
        when(z.getLoan(eq(loanId))).thenReturn(loan);
        final Tenant auth = mockTenant(z);
        final Investor builder = Investor.build(auth);
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        final Investing exec = new Investing(builder, ALL_ACCEPTING, auth);
        final Collection<Investment> result = exec.apply(portfolio, Collections.singleton(ld));
        verify(z, never()).invest(any()); // dry run
        assertThat(result)
                .extracting(Investment::getLoanId)
                .isEqualTo(Collections.singletonList(loanId));
        // re-check; no balance changed, no marketplace changed, nothing should happen
        assertThat(exec.apply(portfolio, Collections.singleton(ld)))
                .isEmpty();
    }
}
