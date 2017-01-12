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

package com.github.triceo.robozonky;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class InvestmentTrackerTest extends AbstractInvestingTest {

    @Test
    public void constructor() {
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor();
        final Collection<LoanDescriptor> lds = Collections.singleton(ld);
        final InvestmentTracker it = new InvestmentTracker(lds, BigDecimal.valueOf(10000));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(it.isSeenBefore(0)).isFalse();
            softly.assertThat(it.getAvailableLoans())
                    .isNotSameAs(lds)
                    .containsExactly(ld);
            softly.assertThat(it.getAllInvestments()).isEmpty();
            softly.assertThat(it.getInvestmentsMade()).isEmpty();
        });
    }

    @Test
    public void registeringPreexistingInvestments() {
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor();
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, AbstractInvestingTest.mockLoanDescriptor());
        final InvestmentTracker it = new InvestmentTracker(lds, BigDecimal.valueOf(10000));
        it.registerExistingInvestments(Collections.singletonList(new Investment(ld.getLoan(),
                Defaults.MINIMUM_INVESTMENT_IN_CZK)));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(it.getAvailableLoans())
                    .hasSize(1)
                    .doesNotContain(ld);
            softly.assertThat(it.getInvestmentsMade()).isEmpty();
        });
    }

    @Test
    public void makeInvestment() {
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor();
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, AbstractInvestingTest.mockLoanDescriptor());
        final InvestmentTracker it = new InvestmentTracker(lds, BigDecimal.valueOf(10000));
        final Investment i = new Investment(ld.getLoan(), 200);
        it.makeInvestment(i);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(it.getAvailableLoans()).isNotEmpty().doesNotContain(ld);
            softly.assertThat(it.getInvestmentsMade()).containsExactly(i);
            softly.assertAll();
        });
    }

    @Test
    public void untouchablesPersistence() {
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor();
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, AbstractInvestingTest.mockLoanDescriptor());
        // ignore the loan and persist
        final InvestmentTracker it = new InvestmentTracker(lds, BigDecimal.valueOf(10000));
        it.discardLoan(ld.getLoan().getId());
        Assertions.assertThat(it.getAvailableLoans()).isNotEmpty().doesNotContain(ld);
        // load it all over again
        final InvestmentTracker it2 = new InvestmentTracker(lds, BigDecimal.valueOf(10000));
        Assertions.assertThat(it2.getAvailableLoans()).isNotEmpty().doesNotContain(ld);
    }

}
