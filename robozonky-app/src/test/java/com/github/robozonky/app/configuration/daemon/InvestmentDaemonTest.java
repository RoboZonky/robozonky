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

package com.github.robozonky.app.configuration.daemon;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestmentDaemonTest extends AbstractZonkyLeveragingTest {

    @Test
    void standard() {
        final int loanId = 1;
        final MarketplaceLoan ml = MarketplaceLoan.custom()
                .setId(loanId)
                .build();
        final Loan l = Loan.custom()
                .setId(loanId)
                .build();
        final Zonky z = harmlessZonky(10_000);
        when(z.getAvailableLoans((Select) notNull())).thenReturn(Stream.of(ml));
        when(z.getLoan(eq(loanId))).thenReturn(l);
        final Authenticated a = mockAuthentication(z);
        final Supplier<Optional<InvestmentStrategy>> s = Optional::empty;
        final InvestingDaemon d = new InvestingDaemon(t -> {
        }, a, new Investor.Builder(), s,
                                                      () -> Optional.of(mock(Portfolio.class)), Duration.ZERO,
                                                      Duration.ofSeconds(1));
        d.run();
        verify(z).getAvailableLoans((Select) notNull());
        verify(z).getLoan(ml.getId());
        assertThat(d.getRefreshInterval()).isEqualByComparingTo(Duration.ofSeconds(1));
    }
}
