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

package com.github.triceo.robozonky.app.investing;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class ResultTrackerTest {

    @Test
    public void nullSafeMarketplace() {
        Assertions.assertThat(new ResultTracker().acceptLoansFromMarketplace(null)).isEmpty();
    }

    @Test
    public void filteringUselessLoans() {
        final Loan usefulLoan = Mockito.mock(Loan.class);
        Mockito.when(usefulLoan.getId()).thenReturn(1);
        Mockito.when(usefulLoan.getRemainingInvestment()).thenReturn(1000.0);
        Mockito.when(usefulLoan.getDatePublished()).thenReturn(OffsetDateTime.now());
        final Loan uselessLoan = Mockito.mock(Loan.class);
        Mockito.when(uselessLoan.getId()).thenReturn(2);
        Mockito.when(uselessLoan.getRemainingInvestment()).thenReturn(0.0);
        final ResultTracker t = new ResultTracker();
        final List<LoanDescriptor> result =
                new ArrayList<>(t.acceptLoansFromMarketplace(Arrays.asList(usefulLoan, uselessLoan)));
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0)).matches(ld -> ld.getLoan().getId() == usefulLoan.getId());
    }

    @Test
    public void filteringLoansAlreadyInvested() {
        final Loan usefulLoan = Mockito.mock(Loan.class);
        Mockito.when(usefulLoan.getId()).thenReturn(1);
        Mockito.when(usefulLoan.getRemainingInvestment()).thenReturn(1000.0);
        Mockito.when(usefulLoan.getDatePublished()).thenReturn(OffsetDateTime.now());
        final Loan uselessLoan = Mockito.mock(Loan.class);
        Mockito.when(uselessLoan.getId()).thenReturn(2);
        Mockito.when(uselessLoan.getRemainingInvestment()).thenReturn(10000.0);
        Mockito.when(uselessLoan.getDatePublished()).thenReturn(OffsetDateTime.now());
        final ResultTracker t = new ResultTracker();
        t.acceptInvestmentsFromRobot(Collections.singleton(new Investment(uselessLoan, 200)));
        final List<LoanDescriptor> result =
                new ArrayList<>(t.acceptLoansFromMarketplace(Arrays.asList(usefulLoan, uselessLoan)));
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0)).matches(ld -> ld.getLoan().getId() == usefulLoan.getId());
    }

}
