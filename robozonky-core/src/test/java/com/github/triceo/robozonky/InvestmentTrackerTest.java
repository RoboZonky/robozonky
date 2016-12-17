/*
 * Copyright 2016 Lukáš Petrovický
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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class InvestmentTrackerTest {

    private static final Random RANDOM = new Random(0);

    private static Loan mockLoan() {
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getId()).thenReturn(InvestmentTrackerTest.RANDOM.nextInt());
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now());
        return loan;
    }

    private static LoanDescriptor mockLoanDescriptor() {
        final Loan loan = InvestmentTrackerTest.mockLoan();
        return new LoanDescriptor(loan, Duration.ofSeconds(30));
    }

    @Test
    public void constructor() {
        final LoanDescriptor ld = InvestmentTrackerTest.mockLoanDescriptor();
        final Collection<LoanDescriptor> lds = Collections.singleton(ld);
        final InvestmentTracker it = new InvestmentTracker(lds);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(it.getAvailableLoans())
                .isNotSameAs(lds)
                .containsExactly(ld);
        softly.assertThat(it.getAllInvestments()).isEmpty();
        softly.assertThat(it.getInvestmentsMade()).isEmpty();
        softly.assertAll();
    }

    @Test
    public void registeringPreexistingInvestments() {
        final LoanDescriptor ld = InvestmentTrackerTest.mockLoanDescriptor();
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, InvestmentTrackerTest.mockLoanDescriptor());
        final InvestmentTracker it = new InvestmentTracker(lds);
        it.registerExistingInvestments(Collections.singletonList(new Investment(ld.getLoan(),
                Defaults.MINIMUM_INVESTMENT_IN_CZK)));
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(it.getAvailableLoans())
                .hasSize(1)
                .doesNotContain(ld);
        softly.assertThat(it.getInvestmentsMade()).isEmpty();
        softly.assertAll();
    }

}
