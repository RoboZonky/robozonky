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

package com.github.robozonky.strategy.natural;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.LoanDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class DefaultValuesTest {

    @Test
    void construct() {
        final DefaultPortfolio p = DefaultPortfolio.BALANCED;
        final DefaultValues sut = new DefaultValues(p);
        assertSoftly(softly -> {
            softly.assertThat(sut.getPortfolio()).isSameAs(p);
            softly.assertThat(sut.getInvestmentSize().getMinimumInvestmentInCzk()).isEqualTo(0);
            softly.assertThat(sut.needsConfirmation(new LoanDescriptor(Loan.custom().build()))).isFalse();
        });
    }

    @Test
    void setMinimumBalance() {
        final DefaultPortfolio p = DefaultPortfolio.BALANCED;
        final DefaultValues sut = new DefaultValues(p);
        assertThat(sut.getMinimumBalance()).isEqualTo(0);
        sut.setMinimumBalance(400);
        assertThat(sut.getMinimumBalance()).isEqualTo(400);
    }

    @Test
    void setTargetPortfolioSize() {
        final DefaultPortfolio p = DefaultPortfolio.PROGRESSIVE;
        final DefaultValues sut = new DefaultValues(p);
        assertThat(sut.getTargetPortfolioSize()).isEqualTo(Long.MAX_VALUE);
        sut.setTargetPortfolioSize(400);
        assertThat(sut.getTargetPortfolioSize()).isEqualTo(400);
    }

    @Test
    void setWrongTargetPortfolioSize() {
        final DefaultPortfolio p = DefaultPortfolio.EMPTY;
        final DefaultValues sut = new DefaultValues(p);
        assertThatThrownBy(() -> sut.setTargetPortfolioSize(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
