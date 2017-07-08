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

package com.github.triceo.robozonky.strategy.natural;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultValuesTest {

    @Test
    public void construct() {
        final DefaultPortfolio p = DefaultPortfolio.BALANCED;
        final DefaultValues sut = new DefaultValues(p);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(sut.getPortfolio()).isSameAs(p);
            softly.assertThat(sut.getInvestmentSize().getMinimumInvestmentInCzk())
                    .isEqualTo(Defaults.MINIMUM_INVESTMENT_IN_CZK);
            softly.assertThat(sut.needsConfirmation(Mockito.mock(Loan.class))).isFalse();
        });
    }

    @Test
    public void setMinimumBalance() {
        final DefaultPortfolio p = DefaultPortfolio.BALANCED;
        final DefaultValues sut = new DefaultValues(p);
        Assertions.assertThat(sut.getMinimumBalance()).isEqualTo(Defaults.MINIMUM_INVESTMENT_IN_CZK);
        sut.setMinimumBalance(400);
        Assertions.assertThat(sut.getMinimumBalance()).isEqualTo(400);
        sut.setMinimumBalance(Defaults.MINIMUM_INVESTMENT_IN_CZK);
        Assertions.assertThat(sut.getMinimumBalance()).isEqualTo(Defaults.MINIMUM_INVESTMENT_IN_CZK);
    }

    @Test
    public void setWrongMinimumBalance() {
        final DefaultPortfolio p = DefaultPortfolio.CONSERVATIVE;
        final DefaultValues sut = new DefaultValues(p);
        Assertions.assertThatThrownBy(() -> sut.setMinimumBalance(Defaults.MINIMUM_INVESTMENT_IN_CZK - 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void setTargetPortfolioSize() {
        final DefaultPortfolio p = DefaultPortfolio.PROGRESSIVE;
        final DefaultValues sut = new DefaultValues(p);
        Assertions.assertThat(sut.getTargetPortfolioSize()).isEqualTo(Integer.MAX_VALUE);
        sut.setTargetPortfolioSize(400);
        Assertions.assertThat(sut.getTargetPortfolioSize()).isEqualTo(400);
    }

    @Test
    public void setWrongTargetPortfolioSize() {
        final DefaultPortfolio p = DefaultPortfolio.EMPTY;
        final DefaultValues sut = new DefaultValues(p);
        Assertions.assertThatThrownBy(() -> sut.setTargetPortfolioSize(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
