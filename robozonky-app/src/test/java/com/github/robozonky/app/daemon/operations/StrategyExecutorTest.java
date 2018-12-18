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
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.EventTenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StrategyExecutorTest extends AbstractZonkyLeveragingTest {

    private static final InvestmentStrategy ALL_ACCEPTING_STRATEGY =
            (a, p, r) -> a.stream()
                    .map(d -> d.recommend(200))
                    .flatMap(q -> q.map(Stream::of).orElse(Stream.empty()));
    private static final Supplier<Optional<InvestmentStrategy>> ALL_ACCEPTING =
            () -> Optional.of(ALL_ACCEPTING_STRATEGY);

    @Test
    void rechecksMarketplaceIfBalanceIncreased() {
        final Zonky zonky = harmlessZonky(10_000);
        final EventTenant auth = mockTenant(zonky);
        final Loan loan = Loan.custom().build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Collection<LoanDescriptor> marketplace = Collections.singleton(ld);
        // prepare the executor, have it fail when executing the investment operation
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> e = new AlwaysFreshNeverInvesting(auth);
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> spied = spy(e);
        assertThat(spied.apply(marketplace)).isEmpty(); // fresh balance, check marketplace
        verify(spied).execute(eq(ALL_ACCEPTING_STRATEGY), eq(marketplace));
        assertThat(spied.apply(marketplace)).isEmpty(); // marketplace first processed
        verify(spied, times(2)).execute(eq(ALL_ACCEPTING_STRATEGY), eq(marketplace));
        assertThat(spied.apply(marketplace)).isEmpty(); // nothing changed, no check
        verify(spied, times(2)).execute(eq(ALL_ACCEPTING_STRATEGY), eq(marketplace));
        when(zonky.getWallet()).thenReturn(new Wallet(BigDecimal.valueOf(100_000))); // increase remote balance
        assertThat(spied.apply(marketplace)).isEmpty(); // should have checked marketplace
        verify(spied, times(3)).execute(eq(ALL_ACCEPTING_STRATEGY), eq(marketplace));
    }

    @Test
    void doesNotInvestOnEmptyMarketplace() {
        final Zonky zonky = harmlessZonky(10_000);
        final EventTenant auth = mockTenant(zonky);
        final StrategyExecutor<LoanDescriptor, InvestmentStrategy> e = spy(new AlwaysFreshNeverInvesting(auth));
        assertThat(e.apply(Collections.emptyList())).isEmpty();
        verify(e, never()).execute(any(), any());
    }

    private static class AlwaysFreshNeverInvesting extends StrategyExecutor<LoanDescriptor, InvestmentStrategy> {

        public AlwaysFreshNeverInvesting(final EventTenant tenant) {
            super(tenant, StrategyExecutorTest.ALL_ACCEPTING);
        }

        @Override
        protected boolean isBalanceUnderMinimum(final int currentBalance) {
            return false;
        }

        @Override
        protected long identify(final LoanDescriptor descriptor) {
            return descriptor.item().getId();
        }

        @Override
        protected Collection<Investment> execute(final InvestmentStrategy strategy,
                                                 final Collection<LoanDescriptor> marketplace) {
            final Tenant t = getTenant();
            return strategy.recommend(marketplace, t.getPortfolio().getOverview(), t.getRestrictions())
                    .map(r -> Investment.fresh(r.descriptor().item(), r.amount().intValue()))
                    .collect(Collectors.toList());
        }
    }
}
