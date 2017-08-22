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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class UtilTest {

    @Test
    public void noBlockedAmounts() {
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getBlockedAmounts()).thenReturn(Stream.empty());
        final Collection<Investment> result = Util.retrieveInvestmentsRepresentedByBlockedAmounts(zonky);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void mergingBlockedAmounts() {
        // prepare data
        final int loanId1 = 1, loanId2 = 2;
        final BigDecimal loan1amount1 = BigDecimal.valueOf(300), loan1amount2 = BigDecimal.valueOf(400),
                loan2amount = BigDecimal.valueOf(500);
        final Loan loan1 = Mockito.mock(Loan.class);
        Mockito.when(loan1.getId()).thenReturn(loanId1);
        final Loan loan2 = Mockito.mock(Loan.class);
        Mockito.when(loan2.getId()).thenReturn(loanId2);
        final Stream<BlockedAmount> blockedAmounts = Stream.of(
                new BlockedAmount(BigDecimal.valueOf(200)), new BlockedAmount(loanId1, loan1amount1),
                new BlockedAmount(BigDecimal.valueOf(300)), new BlockedAmount(loanId2, loan2amount),
                new BlockedAmount(loanId1, loan1amount2) // repeat loan we've already seen, simulating re-invest
        );
        // mock endpoints
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getBlockedAmounts()).thenReturn(blockedAmounts);
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(loanId1))).thenReturn(loan1);
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(loanId2))).thenReturn(loan2);
        // check the loan amounts have been properly merged, investors' fees ignored
        final List<Investment> result = Util.retrieveInvestmentsRepresentedByBlockedAmounts(zonky);
        Assertions.assertThat(result).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.get(0).getAmount()).isEqualTo(loan1amount1.add(loan1amount2));
            softly.assertThat(result.get(1).getAmount()).isEqualTo(loan2amount);
        });
    }
}
