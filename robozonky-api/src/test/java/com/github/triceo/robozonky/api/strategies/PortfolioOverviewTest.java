/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.strategies;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class PortfolioOverviewTest {

    @Test
    public void emptyPortfolio() {
        final int balance = 5000;
        final PortfolioOverview o = PortfolioOverview.calculate(BigDecimal.valueOf(balance), Collections.emptyList());
        SoftAssertions.assertSoftly(softly -> {
            for (final Rating r : Rating.values()) {
                softly.assertThat(o.getShareOnInvestment(r)).isEqualTo(BigDecimal.ZERO);
            }
            softly.assertThat(o.getCzkAvailable()).isEqualTo(balance);
            softly.assertThat(o.getCzkInvested()).isEqualTo(0);
            softly.assertThat(o.getCzkExpectedYield()).isEqualTo(0);
            softly.assertThat(o.getRelativeExpectedYield()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    private static Loan mockLoan(final Rating r) {
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getRating()).thenReturn(r);
        return loan;
    }

    @Test
    public void somePortfolio() {
        final int balance = 5000;
        final Investment i1 = new Investment(mockLoan(Rating.A), 400);
        final Investment i2 = new Investment(mockLoan(Rating.B), 600);
        final PortfolioOverview o = PortfolioOverview.calculate(BigDecimal.valueOf(balance), Arrays.asList(i1, i2));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(o.getShareOnInvestment(Rating.A)).isEqualTo(new BigDecimal("0.4"));
            softly.assertThat(o.getShareOnInvestment(Rating.B)).isEqualTo(new BigDecimal("0.6"));
            softly.assertThat(o.getCzkAvailable()).isEqualTo(balance);
            softly.assertThat(o.getCzkInvested()).isEqualTo(1000);
        });
    }
}
