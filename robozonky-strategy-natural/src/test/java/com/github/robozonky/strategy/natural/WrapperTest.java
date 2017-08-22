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

package com.github.robozonky.strategy.natural;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class WrapperTest {

    @Test
    public void fromInvestment() {
        final Loan loan = new Loan(1, 2);
        final Investment investment = new Investment(loan, 200);
        final Wrapper w = new Wrapper(investment, loan);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(w.getLoanId()).isEqualTo(loan.getId());
            softly.assertThat(w.getStory()).isEqualTo(loan.getStory());
            softly.assertThat(w.getRegion()).isEqualTo(loan.getRegion());
            softly.assertThat(w.getRating()).isEqualTo(loan.getRating());
            softly.assertThat(w.getOriginalAmount()).isEqualTo((int) loan.getAmount());
            softly.assertThat(w.getInterestRate()).isEqualTo(loan.getInterestRate());
            softly.assertThat(w.getRemainingTermInMonths()).isEqualTo(investment.getRemainingMonths());
            softly.assertThat(w.getRemainingAmount()).isEqualTo(investment.getRemainingPrincipal());
            softly.assertThat(w.getIdentifier()).isNotNull();
        });
    }

    @Test
    public void equality() {
        final Loan loan = new Loan(1, 2);
        final Wrapper w = new Wrapper(loan);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(w).isEqualTo(w);
            softly.assertThat(w).isNotEqualTo(null);
            softly.assertThat(w).isNotEqualTo("");
        });
        final Wrapper w2 = new Wrapper(loan);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(w).isEqualTo(w2);
            softly.assertThat(w2).isEqualTo(w);
        });
    }
}
