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
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class LoanAmountConditionTest {

    @Test
    public void leftBoundWrong() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new LoanAmountCondition(-1, 0))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new LoanAmountCondition(0, -1))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new LoanAmountCondition(-1, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    public void boundaryCorrect() {
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getAmount()).thenReturn(0.0);
        final MarketplaceFilterCondition condition = new LoanAmountCondition(0, 0);
        Assertions.assertThat(condition.test(l)).isTrue();
    }

    @Test
    public void leftOutOfBounds() {
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getAmount()).thenReturn(0.0);
        final MarketplaceFilterCondition condition = new LoanAmountCondition(1, 1);
        Assertions.assertThat(condition.test(l)).isFalse();
    }

    @Test
    public void rightOutOfBounds() {
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getAmount()).thenReturn(2.0);
        final MarketplaceFilterCondition condition = new LoanAmountCondition(1, 1);
        Assertions.assertThat(condition.test(l)).isFalse();
    }

}
