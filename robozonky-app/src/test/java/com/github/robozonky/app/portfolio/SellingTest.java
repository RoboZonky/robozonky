/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.portfolio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.api.notifications.SaleRecommendedEvent;
import com.github.robozonky.api.notifications.SaleRequestedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.notifications.SellingStartedEvent;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;
import org.mockito.verification.VerificationMode;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class SellingTest extends AbstractZonkyLeveragingTest {

    private static final SellStrategy ALL_ACCEPTING_STRATEGY =
            (available, portfolio) -> available.stream().map(d -> d.recommend().get());
    private static final SellStrategy NONE_ACCEPTING_STRATEGY = (available, portfolio) -> Stream.empty();
    private static final Supplier<Optional<SellStrategy>> ALL_ACCEPTING = () -> Optional.of(ALL_ACCEPTING_STRATEGY),
            NONE_ACCEPTING = () -> Optional.of(NONE_ACCEPTING_STRATEGY);

    private static Zonky mockApi() {
        final Zonky zonky = mock(Zonky.class);
        when(zonky.getWallet()).thenReturn(new Wallet(BigDecimal.TEN, BigDecimal.ZERO));
        when(zonky.getLoan(anyInt())).thenReturn(Loan.custom().build());
        return zonky;
    }

    private static Investment mockInvestment(final Loan loan) {
        return Investment.fresh(loan, 200)
                .setOriginalTerm(1000)
                .setRemainingPrincipal(BigDecimal.valueOf(100))
                .setStatus(InvestmentStatus.ACTIVE)
                .setOnSmp(false)
                .setOfferable(true)
                .setInWithdrawal(false)
                .build();
    }

    @Test
    void noSaleDueToNoStrategy() {
        new Selling(Optional::empty, true).accept(mock(Portfolio.class), null);
        final List<Event> e = getNewEvents();
        assertThat(e).hasSize(0);
    }

    @Test
    void noSaleDueToNoData() { // no data is inserted into portfolio, therefore nothing happens
        final Zonky zonky = mockApi();
        final Portfolio portfolio = new Portfolio(mockBalance(zonky));
        new Selling(ALL_ACCEPTING, true).accept(portfolio, mockTenant(zonky));
        final List<Event> e = getNewEvents();
        assertThat(e).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(e.get(0)).isInstanceOf(SellingStartedEvent.class);
            softly.assertThat(e.get(1)).isInstanceOf(SellingCompletedEvent.class);
        });
        verify(zonky, never()).sell(any());
    }

    @Test
    void noSaleDueToStrategyForbidding() {
        final Loan loan = Loan.custom()
                .setId(1)
                .build();
        final Investment i = mockInvestment(loan);
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getLoan(eq(1))).thenReturn(loan);
        final Portfolio portfolio = new Portfolio(Statistics.empty(), new TransactionLog(), new int[0],
                                                  mockBalance(zonky));
        new Selling(NONE_ACCEPTING, true).accept(portfolio, mockTenant(zonky));
        final List<Event> e = getNewEvents();
        assertThat(e).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(e.get(0)).isInstanceOf(SellingStartedEvent.class);
            softly.assertThat(e.get(1)).isInstanceOf(SellingCompletedEvent.class);
        });
        verify(zonky, never()).sell(eq(i));
    }

    private void saleMade(final boolean isDryRun) {
        final Loan loan = Loan.custom()
                .setId(1)
                .build();
        final Investment i = mockInvestment(loan);
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getLoan(eq(1))).thenReturn(loan);
        when(zonky.getInvestments((Select) any())).thenReturn(Stream.of(i));
        final Portfolio portfolio = new Portfolio(Statistics.empty(), new TransactionLog(), new int[0],
                                                  mockBalance(zonky));
        new Selling(ALL_ACCEPTING, isDryRun).accept(portfolio, mockTenant(zonky));
        final List<Event> e = getNewEvents();
        assertThat(e).hasSize(5);
        assertSoftly(softly -> {
            softly.assertThat(e.get(0)).isInstanceOf(SellingStartedEvent.class);
            softly.assertThat(e.get(1)).isInstanceOf(SaleRecommendedEvent.class);
            softly.assertThat(e.get(2)).isInstanceOf(SaleRequestedEvent.class);
            softly.assertThat(e.get(3)).isInstanceOf(SaleOfferedEvent.class);
            softly.assertThat(e.get(4)).isInstanceOf(SellingCompletedEvent.class);
        });
        final VerificationMode m = isDryRun ? never() : times(1);
        verify(zonky, m).sell(argThat(inv -> i.getLoanId() == inv.getLoanId()));
    }

    @Test
    void saleMade() {
        saleMade(false);
    }

    @Test
    void saleMadeDryRun() {
        saleMade(true);
    }
}
