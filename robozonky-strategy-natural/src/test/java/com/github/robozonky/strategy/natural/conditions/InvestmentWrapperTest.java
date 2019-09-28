/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.strategy.natural.conditions;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.strategy.natural.Wrapper;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

class InvestmentWrapperTest {

    private static final Loan LOAN = new MockLoanBuilder()
            .setInsuranceActive(true)
            .setAmount(100_000)
            .setRating(Rating.D)
            .setInterestRate(Ratio.ONE)
            .setMainIncomeType(MainIncomeType.EMPLOYMENT)
            .setPurpose(Purpose.AUTO_MOTO)
            .setRegion(Region.JIHOCESKY)
            .setStory(UUID.randomUUID().toString())
            .setTermInMonths(20)
            .build();
    private static final Investment INVESTMENT = MockInvestmentBuilder.fresh(LOAN, 2_000).build();
    private static final PortfolioOverview FOLIO = mock(PortfolioOverview.class);

    @Test
    void values() {
        final InvestmentDescriptor original = new InvestmentDescriptor(INVESTMENT, () -> LOAN);
        final Wrapper<InvestmentDescriptor> w = Wrapper.wrap(original, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.isInsuranceActive()).isEqualTo(INVESTMENT.isInsuranceActive());
            softly.assertThat(w.getInterestRate()).isEqualTo(Ratio.ONE);
            softly.assertThat(w.getRegion()).isEqualTo(LOAN.getRegion());
            softly.assertThat(w.getRating()).isEqualTo(INVESTMENT.getRating());
            softly.assertThat(w.getMainIncomeType()).isEqualTo(LOAN.getMainIncomeType());
            softly.assertThat(w.getPurpose()).isEqualTo(LOAN.getPurpose());
            softly.assertThat(w.getOriginalAmount()).isEqualTo(LOAN.getAmount().getValue().intValue());
            softly.assertThat(w.getRemainingPrincipal()).isEqualTo(INVESTMENT.getRemainingPrincipal().get().getValue());
            softly.assertThat(w.getOriginal()).isSameAs(original);
            softly.assertThat(w.getStory()).isEqualTo(LOAN.getStory());
            softly.assertThat(w.getOriginalTermInMonths()).isEqualTo(INVESTMENT.getLoanTermInMonth());
            softly.assertThat(w.getRemainingTermInMonths()).isEqualTo(INVESTMENT.getRemainingMonths());
            softly.assertThat(w.toString()).isNotNull();
        });
    }

    @Test
    void equality() {
        final InvestmentDescriptor original = new InvestmentDescriptor(INVESTMENT, () -> LOAN);
        final Wrapper<InvestmentDescriptor> w = Wrapper.wrap(original, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w).isEqualTo(w);
            softly.assertThat(w).isEqualTo(Wrapper.wrap(original, FOLIO));
            softly.assertThat(w).isEqualTo(Wrapper.wrap(new InvestmentDescriptor(INVESTMENT, () -> LOAN), FOLIO));
            softly.assertThat(w).isNotEqualTo(null);
        });
    }
}
