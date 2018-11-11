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

package com.github.robozonky.app.daemon;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.daemon.operations.Investor;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvestmentDaemonTest extends AbstractZonkyLeveragingTest {

    @Test
    void standard() {
        final int loanId = 1;
        final MarketplaceLoan ml = MarketplaceLoan.custom()
                .setId(loanId)
                .setRating(Rating.A)
                .build();
        final Loan l = Loan.custom()
                .setId(loanId)
                .setRating(Rating.A)
                .build();
        final Zonky z = harmlessZonky(200);
        when(z.getAvailableLoans(notNull())).thenReturn(Stream.of(ml));
        when(z.getLoan(eq(loanId))).thenReturn(l);
        final Tenant a = mockTenant(z);
        final Portfolio portfolio = Portfolio.create(a, BlockedAmountProcessor.createLazy(a));
        final InvestmentStrategy is = mock(InvestmentStrategy.class);
        final Supplier<Optional<InvestmentStrategy>> s = () -> Optional.of(is);
        final InvestingDaemon d = new InvestingDaemon(t -> {
        }, a, Investor.build(a), s, () -> Optional.of(portfolio), Duration.ofSeconds(1));
        d.run();
        verify(z).getAvailableLoans(notNull());
        verify(is).recommend(any(), any(), any());
        assertThat(d.getRefreshInterval()).isEqualByComparingTo(Duration.ofSeconds(1));
    }

    @Test
    void underBalance() {
        final Zonky z = harmlessZonky(199);
        final Tenant a = mockTenant(z);
        final Portfolio portfolio = Portfolio.create(a, BlockedAmountProcessor.createLazy(a));
        final Supplier<Optional<InvestmentStrategy>> s = Optional::empty;
        final InvestingDaemon d = new InvestingDaemon(t -> {
        }, a, Investor.build(a), s, () -> Optional.of(portfolio), Duration.ofSeconds(1));
        d.run();
        verify(z, never()).getAvailableLoans(notNull());
    }
}
