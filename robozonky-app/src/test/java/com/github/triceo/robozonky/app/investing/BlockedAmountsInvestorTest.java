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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.BlockedAmount;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class BlockedAmountsInvestorTest {

    @Test
    public void noBlockedAmounts() {
        final ZonkyProxy api = Mockito.mock(ZonkyProxy.class);
        Mockito.when(api.execute(ArgumentMatchers.any())).thenReturn(Collections.emptyList());
        final Collection<Investment> result = Investor.retrieveInvestmentsRepresentedByBlockedAmounts(api);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void mergingBlockedAmounts() {
        // prepare data
        final int loanId1 = 1, loanId2 = 2, investorsFee = 0;
        final int loan1amount1 = 300, loan1amount2 = 400, loan2amount = 500;
        final Loan loan1 = Mockito.mock(Loan.class);
        Mockito.when(loan1.getId()).thenReturn(loanId1);
        final Loan loan2 = Mockito.mock(Loan.class);
        Mockito.when(loan2.getId()).thenReturn(loanId2);
        final List<BlockedAmount> blockedAmounts = Arrays.asList(
            new BlockedAmount(investorsFee, 200),
                new BlockedAmount(loanId1, loan1amount1),
                new BlockedAmount(investorsFee, 300),
                new BlockedAmount(loanId2, loan2amount),
                new BlockedAmount(loanId1, loan1amount2) // repeat loan we've already seen, simulating re-invest
        );
        // mock endpoints
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getBlockedAmounts(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                .thenReturn(blockedAmounts);
        Mockito.when(api.getLoan(ArgumentMatchers.eq(loanId1))).thenReturn(loan1);
        Mockito.when(api.getLoan(ArgumentMatchers.eq(loanId2))).thenReturn(loan2);
        final ZonkyProxy proxy = new ZonkyProxy.Builder().build(api);
        // check the loan amounts have been properly merged, investors' fees ignored
        final List<Investment> result = Investor.retrieveInvestmentsRepresentedByBlockedAmounts(proxy);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result).hasSize(2);
        softly.assertThat(result.get(0).getAmount()).isEqualTo(loan1amount1 + loan1amount2);
        softly.assertThat(result.get(1).getAmount()).isEqualTo(loan2amount);
        softly.assertAll();
    }

}
