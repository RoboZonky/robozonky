/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.ws.rs.BadRequestException;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.PurchaseRecommendedEvent;
import com.github.robozonky.api.notifications.PurchaseRequestedEvent;
import com.github.robozonky.api.notifications.PurchasingCompletedEvent;
import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.util.DateUtil;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StrategyExecutorTest extends AbstractZonkyLeveragingTest {

    private static final PurchaseStrategy NONE_ACCEPTING_PURCHASE_STRATEGY = (a, p, r) -> Stream.empty();
    private static final PurchaseStrategy ALL_ACCEPTING_PURCHASE_STRATEGY =
            (a, p, r) -> a.stream().map(d -> d.recommend().get());
    private static final InvestmentStrategy NONE_ACCEPTING_INVESTMENT_STRATEGY = (a, p, r) -> Stream.empty();
    private static final InvestmentStrategy ALL_ACCEPTING_INVESTMENT_STRATEGY =
            (a, p, r) -> a.stream().map(d -> d.recommend(200).get());

    private static PurchasingOperationDescriptor mockPurchasingOperationDescriptor(
            final ParticipationDescriptor... pd) {
        final PurchasingOperationDescriptor d = new PurchasingOperationDescriptor();
        final PurchasingOperationDescriptor spied = spy(d);
        final MarketplaceAccessor<ParticipationDescriptor> marketplace = mock(MarketplaceAccessor.class);
        when(marketplace.getMarketplace()).thenReturn(pd.length == 0 ? Collections.emptyList() : Arrays.asList(pd));
        when(marketplace.hasUpdates()).thenReturn(true);
        doReturn(marketplace).when(spied).newMarketplaceAccessor(any());
        return spied;
    }

    private static InvestingOperationDescriptor mockInvestingOperationDescriptor(final LoanDescriptor... ld) {
        return mockInvestingOperationDescriptor(null, ld);
    }

    private static InvestingOperationDescriptor mockInvestingOperationDescriptor(final Investor investor,
                                                                                 final LoanDescriptor... ld) {
        final InvestingOperationDescriptor d = new InvestingOperationDescriptor(investor);
        final InvestingOperationDescriptor spied = spy(d);
        final MarketplaceAccessor<LoanDescriptor> marketplace = mock(MarketplaceAccessor.class);
        when(marketplace.getMarketplace()).thenReturn(ld.length == 0 ? Collections.emptyList() : Arrays.asList(ld));
        when(marketplace.hasUpdates()).thenReturn(true);
        doReturn(marketplace).when(spied).newMarketplaceAccessor(any());
        return spied;
    }

    @Test
    void purchasingNoStrategy() {
        final Participation mock = mock(Participation.class);
        final ParticipationDescriptor pd = new ParticipationDescriptor(mock, () -> Loan.custom().build());
        final PowerTenant tenant = mockTenant();
        final PurchasingOperationDescriptor d = mockPurchasingOperationDescriptor(pd);
        final StrategyExecutor<ParticipationDescriptor, PurchaseStrategy> exec = new StrategyExecutor<>(tenant, d);
        assertThat(exec.get()).isEmpty();
        // check events
        final List<Event> events = getEventsRequested();
        assertThat(events).isEmpty();
    }

    @Test
    void purchasingNoneAccepted() {
        final Zonky zonky = harmlessZonky(9_000);
        final Loan loan = Loan.custom().setId(1).setAmount(200).build();
        when(zonky.getLoan(eq(loan.getId()))).thenReturn(loan);
        final Participation mock = mock(Participation.class);
        when(mock.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(250));
        final ParticipationDescriptor pd = new ParticipationDescriptor(mock, () -> loan);
        final PowerTenant tenant = mockTenant(zonky);
        when(tenant.getPurchaseStrategy()).thenReturn(Optional.of(NONE_ACCEPTING_PURCHASE_STRATEGY));
        final PurchasingOperationDescriptor d = mockPurchasingOperationDescriptor(pd);
        final StrategyExecutor<ParticipationDescriptor, PurchaseStrategy> exec = new StrategyExecutor<>(tenant, d);
        assertThat(exec.get()).isEmpty();
        final List<Event> e = getEventsRequested();
        assertThat(e).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(e).first().isInstanceOf(PurchasingStartedEvent.class);
            softly.assertThat(e).last().isInstanceOf(PurchasingCompletedEvent.class);
        });
    }

    @Test
    void purchasingSomeAccepted() {
        final int loanId = 1;
        final Loan loan = Loan.custom()
                .setId(loanId)
                .setAmount(100_000)
                .setRating(Rating.D)
                .setNonReservedRemainingInvestment(1000)
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
        final PowerTenant tenant = mockTenant(zonky);
        when(tenant.getPurchaseStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_PURCHASE_STRATEGY));
        final PurchasingOperationDescriptor d = mockPurchasingOperationDescriptor(pd);
        final StrategyExecutor<ParticipationDescriptor, PurchaseStrategy> exec = new StrategyExecutor<>(tenant, d);
        assertThat(exec.get()).isNotEmpty();
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
        assertThat(exec.get()).isEmpty();
    }

    @Test
    void tryPurchaseButZonkyFail() {
        final int loanId = 1;
        final Loan loan = Loan.custom()
                .setId(loanId)
                .setAmount(100_000)
                .setRating(Rating.D)
                .setNonReservedRemainingInvestment(1000)
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
        final PowerTenant tenant = mockTenant(zonky, false);
        when(tenant.getPurchaseStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_PURCHASE_STRATEGY));
        final PurchasingOperationDescriptor d = mockPurchasingOperationDescriptor(pd);
        final StrategyExecutor<ParticipationDescriptor, PurchaseStrategy> exec = new StrategyExecutor<>(tenant, d);
        assertThat(exec.get()).isEmpty();
    }

    @Test
    void purchasingNoItems() {
        final Zonky zonky = harmlessZonky(10_000);
        final PowerTenant tenant = mockTenant(zonky);
        final PurchasingOperationDescriptor d = mockPurchasingOperationDescriptor();
        final StrategyExecutor<ParticipationDescriptor, PurchaseStrategy> exec = new StrategyExecutor<>(tenant, d);
        when(tenant.getPurchaseStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_PURCHASE_STRATEGY));
        assertThat(exec.get()).isEmpty();
        final List<Event> e = getEventsRequested();
        assertThat(e).isEmpty();
    }

    @Test
    void investingNoStrategy() {
        final int loanId = (int) (Math.random() * 1000); // new ID every time to avoid caches
        final Loan loan = Loan.custom()
                .setId(loanId)
                .setAmount(2)
                .build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(1000);
        final PowerTenant tenant = mockTenant(z);
        final InvestingOperationDescriptor d = mockInvestingOperationDescriptor(ld);
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> exec = new StrategyExecutor<>(tenant, d);
        assertThat(exec.get()).isEmpty();
        // check events
        final List<Event> events = getEventsRequested();
        assertThat(events).isEmpty();
    }

    @Test
    void investingNoItems() {
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(1000);
        final PowerTenant tenant = mockTenant(z);
        when(tenant.getInvestmentStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_INVESTMENT_STRATEGY));
        final Investor builder = Investor.build(tenant);
        final InvestingOperationDescriptor d = mockInvestingOperationDescriptor(builder);
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> exec = new StrategyExecutor<>(tenant, d);
        assertThat(exec.get()).isEmpty();
    }

    @Test
    void investingNoneAccepted() {
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
        final PowerTenant tenant = mockTenant(z);
        when(tenant.getInvestmentStrategy()).thenReturn(Optional.of(NONE_ACCEPTING_INVESTMENT_STRATEGY));
        final Investor builder = Investor.build(tenant);
        when(z.getLoan(eq(loanId))).thenReturn(loan);
        final InvestingOperationDescriptor d = mockInvestingOperationDescriptor(builder, ld);
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> exec = new StrategyExecutor<>(tenant, d);
        assertThat(exec.get()).isEmpty();
    }

    @Test
    void investingRejected() {
        final Loan loan = Loan.custom()
                .setAmount(100_000)
                .setRating(Rating.D)
                .setNonReservedRemainingInvestment(100_000)
                .setMyInvestment(mockMyInvestment())
                .setDatePublished(OffsetDateTime.now())
                .build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Zonky z = harmlessZonky(9000);
        final PowerTenant tenant = mockTenant(z);
        when(tenant.getInvestmentStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_INVESTMENT_STRATEGY));
        final Investor investor = mock(Investor.class);
        when(investor.getConfirmationProvider()).thenReturn(Optional.of(mock(ConfirmationProvider.class)));
        when(investor.invest(any(), anyBoolean())).thenReturn(Either.left(InvestmentFailure.REJECTED));
        when(z.getLoan(eq(loan.getId()))).thenReturn(loan);
        final InvestingOperationDescriptor d = mockInvestingOperationDescriptor(investor, ld);
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> exec = new StrategyExecutor<>(tenant, d);
        assertThat(exec.get()).isEmpty();
        verify(investor, times(1)).invest(any(), anyBoolean()); // call to invest
        verify(z, never()).invest(any()); // does not propagate to Zonky
        // now try again, the same loan should now be discarded and therefore not even attempted
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> exec2 = new StrategyExecutor<>(tenant, d);
        assertThat(exec2.get()).isEmpty();
        verify(investor, times(1)).invest(any(), anyBoolean()); // call to invest
        final List<Event> evt = getEventsRequested();
        assertThat(evt).isNotEmpty();
    }

    @Test
    void investingSomeAccepted() {
        final Loan loan = Loan.custom()
                .setAmount(100_000)
                .setRating(Rating.C)
                .setNonReservedRemainingInvestment(20_000)
                .build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Zonky z = harmlessZonky(10_000);
        final int loanId = loan.getId(); // will be random, to avoid problems with caching
        when(z.getLoan(eq(loanId))).thenReturn(loan);
        final PowerTenant tenant = mockTenant(z);
        when(tenant.getInvestmentStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_INVESTMENT_STRATEGY));
        final Investor builder = Investor.build(tenant);
        final InvestingOperationDescriptor d = mockInvestingOperationDescriptor(builder, ld);
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> exec = new StrategyExecutor<>(tenant, d);
        final Collection<Investment> result = exec.get();
        verify(z, never()).invest(any()); // dry run
        assertThat(result)
                .extracting(Investment::getLoanId)
                .isEqualTo(Collections.singletonList(loanId));
        // re-check; no balance changed, no marketplace changed, nothing should happen
        assertThat(exec.get()).isEmpty();
        final List<Event> evt = getEventsRequested();
        assertThat(evt).isNotEmpty();
    }

    @Test
    void doesNotInvestUnderBalance() {
        final Loan loan = Loan.custom().build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Zonky zonky = harmlessZonky(1);
        final PowerTenant tenant = mockTenant(zonky);
        final InvestingOperationDescriptor d = mockInvestingOperationDescriptor(ld);
        doReturn(true).when(d).isEnabled(any());
        doReturn(BigDecimal.ONE).when(d).getMinimumBalance(any());
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> e = new StrategyExecutor<>(tenant, d);
        assertThat(e.get()).isEmpty();
        verify(d, never()).getOperation();
        final List<Event> evt = getEventsRequested();
        assertThat(evt).isEmpty();
    }

    @Test
    void doesNotInvestWhenDisabled() {
        final Zonky zonky = harmlessZonky(10_000);
        final PowerTenant tenant = mockTenant(zonky);
        final OperationDescriptor<LoanDescriptor, InvestmentStrategy> d = mock(OperationDescriptor.class);
        when(d.isEnabled(any())).thenReturn(false);
        when(d.newJfrEvent()).thenReturn(mock(jdk.jfr.Event.class));
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> e = new StrategyExecutor<>(tenant, d);
        assertThat(e.get()).isEmpty();
        verify(d, never()).newMarketplaceAccessor(any());
        final List<Event> evt = getEventsRequested();
        assertThat(evt).isEmpty();
    }

    @Test
    void forcesMarketplaceCheck() {
        final Instant now = Instant.now();
        setClock(Clock.fixed(now, Defaults.ZONE_ID));
        final Zonky zonky = harmlessZonky(10_000);
        final Loan loan = Loan.custom()
                .setDatePublished(DateUtil.offsetNow().minusMinutes(10)) // avoid CAPTCHA
                .setRating(Rating.D)
                .build();
        when(zonky.getAvailableLoans(any())).thenAnswer(i -> Stream.of(loan));
        final PowerTenant tenant = mockTenant(zonky);
        when(tenant.getInvestmentStrategy())
                .thenReturn(Optional.of((available, portfolio, restrictions) -> Stream.empty()));
        final Investor investor = Investor.build(tenant);
        final OperationDescriptor<LoanDescriptor, InvestmentStrategy> d = new InvestingOperationDescriptor(investor);
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> e = new StrategyExecutor<>(tenant, d);
        assertThat(e.get()).isEmpty();
        verify(zonky, never()).getLastPublishedLoanInfo();
        verify(zonky, times(1)).getAvailableLoans(any());
        assertThat(e.get()).isEmpty(); // the second time, marketplace wasn't checked
        verify(zonky, times(1)).getLastPublishedLoanInfo();
        verify(zonky, times(1)).getAvailableLoans(any());
        assertThat(e.get()).isEmpty(); // the third time, marketplace wasn't checked but the cache was
        verify(zonky, times(2)).getLastPublishedLoanInfo();
        verify(zonky, times(1)).getAvailableLoans(any());
        setClock(Clock.fixed(now.plus(Duration.ofMinutes(1)), Defaults.ZONE_ID));
        assertThat(e.get()).isEmpty(); // after 1 minute, marketplace was force-checked
        verify(zonky, times(3)).getLastPublishedLoanInfo();
        verify(zonky, times(2)).getAvailableLoans(any());
    }
}
