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

package com.github.robozonky.app.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StrategyExecutorTest extends AbstractZonkyLeveragingTest {

    private static final InvestmentStrategy ALL_ACCEPTING_STRATEGY =
            (a, p, r) -> a.stream().map(d -> d.recommend(200).get());
    private static final Supplier<Optional<InvestmentStrategy>> ALL_ACCEPTING =
            () -> Optional.of(ALL_ACCEPTING_STRATEGY);

    @Test
    void rechecksMarketplaceIfFirstCheckFailed() {
        final Zonky zonky = harmlessZonky(10_000);
        final Portfolio p = Portfolio.create(mockAuthentication(zonky), mockBalance(zonky));
        final Loan loan = Loan.custom().build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Collection<LoanDescriptor> marketplace = Collections.singleton(ld);
        // prepare the executor, have it fail when executing the investment operation
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> e = new AlwaysFreshNeverInvesting();
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> mocked = spy(e);
        doThrow(new IllegalStateException()).when(mocked).execute(any(), any(), any());
        Assertions.assertThatThrownBy(() -> mocked.apply(p, marketplace)).isInstanceOf(IllegalStateException.class);
        reset(mocked); // now the method won't throw, but must be executed again
        mocked.apply(p, marketplace);
        verify(mocked).execute(eq(p), eq(ALL_ACCEPTING_STRATEGY), eq(marketplace));
        // marketplace stays the same + balance stays the same+  previous update passed = no update should be triggered
        reset(mocked);
        when(mocked.hasMarketplaceUpdates(eq(marketplace))).thenReturn(false);
        mocked.apply(p, marketplace);
        verify(mocked, never()).execute(any(), any(), any());
    }

    @Test
    void rechecksMarketplaceIfBalanceIncreased() {
        final Zonky zonky = harmlessZonky(10_000);
        final Portfolio p = Portfolio.create(mockAuthentication(zonky), mockBalance(zonky));
        final Loan loan = Loan.custom().build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Collection<LoanDescriptor> marketplace = Collections.singleton(ld);
        // prepare the executor, have it fail when executing the investment operation
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> e = new AlwaysFreshNeverInvesting();
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> mocked = spy(e);
        when(mocked.hasMarketplaceUpdates(any())).thenReturn(false); // marketplace never has any updates
        mocked.apply(p, marketplace); // fresh balance, check marketplace
        verify(mocked).execute(eq(p), eq(ALL_ACCEPTING_STRATEGY), eq(marketplace));
        mocked.apply(p, marketplace); // nothing changed, still only ran once
        verify(mocked, times(1)).execute(eq(p), eq(ALL_ACCEPTING_STRATEGY), eq(marketplace));
        when(zonky.getWallet()).thenReturn(new Wallet(BigDecimal.valueOf(100_000))); // increase remote balance
        mocked.apply(p, marketplace); // should have checked marketplace
        verify(mocked, times(2)).execute(eq(p), eq(ALL_ACCEPTING_STRATEGY), eq(marketplace));
    }

    @Test
    void doesNotInvestOnEmptyMarketplace() {
        final Zonky zonky = harmlessZonky(10_000);
        final Portfolio p = Portfolio.create(mockAuthentication(zonky), mockBalance(zonky));
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> e = spy(new AlwaysFreshNeverInvesting());
        e.apply(p, Collections.emptyList());
        verify(e, never()).execute(any(), any(), any());
    }

    private static class AlwaysFreshNeverInvesting extends StrategyExecutor<LoanDescriptor, InvestmentStrategy> {

        public AlwaysFreshNeverInvesting() {
            super(StrategyExecutorTest.ALL_ACCEPTING);
        }

        @Override
        protected boolean isBalanceUnderMinimum(final int currentBalance) {
            return false;
        }

        @Override
        protected boolean hasMarketplaceUpdates(final Collection<LoanDescriptor> marketplace) {
            return false;
        }

        @Override
        protected Collection<Investment> execute(final Portfolio portfolio, final InvestmentStrategy strategy,
                                                 final Collection<LoanDescriptor> marketplace) {
            return Collections.emptyList();
        }
    }
}
