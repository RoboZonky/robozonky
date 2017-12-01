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

import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class PortfolioLoanProviderTest {

    @Test
    public void noPortfolio() {
        final Supplier<Optional<Portfolio>> portfolioProvider = Optional::empty;
        final LoanProvider p = new PortfolioLoanProvider(portfolioProvider);
        Assertions.assertThatThrownBy(() -> p.apply(0, Mockito.mock(Zonky.class)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void proper() {
        final Supplier<Optional<Portfolio>> portfolioProvider = () -> Optional.of(new Portfolio());
        final Zonky zonky = Mockito.mock(Zonky.class);
        final Loan l = new Loan(1, 200);
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(l.getId()))).thenReturn(l);
        final LoanProvider p = new PortfolioLoanProvider(portfolioProvider);
        Assertions.assertThat(p.apply(l.getId(), zonky)).isSameAs(l);
    }
}
