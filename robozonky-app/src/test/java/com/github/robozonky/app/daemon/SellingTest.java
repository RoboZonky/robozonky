/*
 * Copyright 2020 The RoboZonky Project
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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.InternalServerErrorException;

import org.junit.jupiter.api.Test;
import org.mockito.verification.VerificationMode;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.api.notifications.SaleRecommendedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.notifications.SellingStartedEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.remote.entities.SellInfoImpl;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;

class SellingTest extends AbstractZonkyLeveragingTest {

    private static final SellStrategy ALL_ACCEPTING_STRATEGY = (available, portfolio, sessionInfo) -> available.stream()
        .map(d -> d.recommend()
            .get());
    private static final SellStrategy NONE_ACCEPTING_STRATEGY = (available, portfolio, sessionInfo) -> Stream.empty();

    private static Investment mockInvestment(final Loan loan) {
        return mockInvestment(loan, LoanHealth.HEALTHY);
    }

    private static Investment mockInvestment(final Loan loan, final LoanHealth loanHealth) {
        return MockInvestmentBuilder.fresh(loan, 200)
            .set(InvestmentImpl::setRating, Rating.AAAAA)
            .set(InvestmentImpl::setLoanTermInMonth, 1000)
            .set(InvestmentImpl::setLoanHealthInfo, loanHealth)
            .set(InvestmentImpl::setRemainingPrincipal, Money.from(BigDecimal.valueOf(100)))
            .set(InvestmentImpl::setStatus, InvestmentStatus.ACTIVE)
            .set(InvestmentImpl::setOnSmp, false)
            .build();
    }

    @Test
    void noSaleDueToNoStrategy() {
        new Selling().accept(mockTenant());
        final List<Event> e = getEventsRequested();
        assertThat(e).hasSize(0);
    }

    @Test
    void noSaleDueToNoData() { // no data is inserted into portfolio, therefore nothing happens
        final Zonky zonky = harmlessZonky();
        final PowerTenant tenant = mockTenant(zonky);
        when(tenant.getSellStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        new Selling().accept(tenant);
        final List<Event> e = getEventsRequested();
        assertThat(e).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(e.get(0))
                .isInstanceOf(SellingStartedEvent.class);
            softly.assertThat(e.get(1))
                .isInstanceOf(SellingCompletedEvent.class);
        });
        verify(zonky, never()).sell(any());
    }

    @Test
    void noSaleDueToStrategyForbidding() {
        final Loan loan = MockLoanBuilder.fresh();
        final Investment i = mockInvestment(loan);
        final Zonky zonky = harmlessZonky();
        when(zonky.getLoan(eq(1))).thenReturn(loan);
        final PowerTenant tenant = mockTenant(zonky);
        when(tenant.getSellStrategy()).thenReturn(Optional.of(NONE_ACCEPTING_STRATEGY));
        new Selling().accept(tenant);
        final List<Event> e = getEventsRequested();
        assertThat(e).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(e.get(0))
                .isInstanceOf(SellingStartedEvent.class);
            softly.assertThat(e.get(1))
                .isInstanceOf(SellingCompletedEvent.class);
        });
        verify(zonky, never()).sell(eq(i));
    }

    @Test
    void noSaleDueToHttp500Error() {
        final Loan loan = MockLoanBuilder.fresh();
        final Investment i = mockInvestment(loan);
        final Zonky zonky = harmlessZonky();
        doThrow(InternalServerErrorException.class).when(zonky)
            .sell(any());
        when(zonky.getLoan(eq(loan.getId()))).thenReturn(loan);
        when(zonky.getInvestments(any())).thenAnswer(inv -> Stream.of(i));
        when(zonky.getSoldInvestments()).thenAnswer(inv -> Stream.empty());
        final PowerTenant tenant = mockTenant(zonky, false);
        when(tenant.getSellStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        final Selling s = new Selling();
        s.accept(tenant);
        final List<Event> e = getEventsRequested();
        assertThat(e).hasSize(3);
        assertSoftly(softly -> {
            softly.assertThat(e.get(0))
                .isInstanceOf(SellingStartedEvent.class);
            softly.assertThat(e.get(1))
                .isInstanceOf(SaleRecommendedEvent.class);
            softly.assertThat(e.get(2))
                .isInstanceOf(SellingCompletedEvent.class);
        });
        verify(zonky, times(1)).sell(argThat(inv -> i.getLoanId() == inv.getLoanId()));
    }

    private void saleMade(final boolean isDryRun, final boolean healthy) {
        final Loan loan = new MockLoanBuilder().build();
        final Investment i = mockInvestment(loan, healthy ? LoanHealth.HEALTHY : LoanHealth.HISTORICALLY_IN_DUE);
        final Zonky zonky = harmlessZonky();
        when(zonky.getLoan(eq(loan.getId()))).thenReturn(loan);
        SellInfo sellInfo = mock(SellInfoImpl.class);
        when(zonky.getSellInfo(eq(i.getId()))).thenReturn(sellInfo);
        when(zonky.getInvestments(any())).thenAnswer(inv -> Stream.of(i));
        when(zonky.getSoldInvestments()).thenAnswer(inv -> Stream.empty());
        final PowerTenant tenant = mockTenant(zonky, isDryRun);
        when(tenant.getSellStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        final Selling s = new Selling();
        s.accept(tenant);
        final List<Event> e = getEventsRequested();
        assertThat(e).hasSize(4);
        assertSoftly(softly -> {
            softly.assertThat(e.get(0))
                .isInstanceOf(SellingStartedEvent.class);
            softly.assertThat(e.get(1))
                .isInstanceOf(SaleRecommendedEvent.class);
            softly.assertThat(e.get(2))
                .isInstanceOf(SaleOfferedEvent.class);
            softly.assertThat(e.get(3))
                .isInstanceOf(SellingCompletedEvent.class);
        });
        final VerificationMode m = isDryRun ? never() : times(1);
        if (healthy) {
            verify(zonky, m).sell(argThat(inv -> i.getLoanId() == inv.getLoanId()));
            verify(zonky, never()).sell(argThat(inv -> i.getLoanId() == inv.getLoanId()), eq(sellInfo));
        } else {
            verify(zonky, never()).sell(argThat(inv -> i.getLoanId() == inv.getLoanId()));
            verify(zonky, m).sell(argThat(inv -> i.getLoanId() == inv.getLoanId()), eq(sellInfo));
        }
        // try to sell the same thing again, make sure it doesn't happen
        readPreexistingEvents();
        s.accept(tenant);
        if (healthy) {
            verify(zonky, m).sell(argThat(inv -> i.getLoanId() == inv.getLoanId()));
            verify(zonky, never()).sell(argThat(inv -> i.getLoanId() == inv.getLoanId()), eq(sellInfo));
        } else {
            verify(zonky, never()).sell(argThat(inv -> i.getLoanId() == inv.getLoanId()));
            verify(zonky, m).sell(argThat(inv -> i.getLoanId() == inv.getLoanId()), eq(sellInfo));
        }
    }

    @Test
    void saleMade() {
        saleMade(false, true);
    }

    @Test
    void saleMadeDryRun() {
        saleMade(true, true);
    }

    @Test
    void saleMadeUnhealthy() {
        saleMade(false, false);
    }

    @Test
    void saleMadeUnhealthyDryRun() {
        saleMade(true, false);
    }
}
