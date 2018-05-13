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

package com.github.robozonky.strategy.natural.conditions;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.strategy.natural.LoanWrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

class LoanAmountConditionTest {

    private static LoanWrapper mockWrapper(final int originalAmount) {
        return new LoanWrapper(Loan.custom()
                                       .setAmount(originalAmount)
                                       .build());
    }

    @Test
    void leftBoundWrong() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new LoanAmountCondition(-1, 0))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new LoanAmountCondition(0, -1))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new LoanAmountCondition(-1, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void boundaryCorrect() {
        final LoanWrapper l = mockWrapper(0);
        final MarketplaceFilterConditionImpl condition = new LoanAmountCondition(0, 0);
        assertThat(condition.test(l)).isTrue();
    }

    @Test
    void leftOutOfBounds() {
        final LoanWrapper l = mockWrapper(0);
        final MarketplaceFilterConditionImpl condition = new LoanAmountCondition(1, 1);
        assertThat(condition.test(l)).isFalse();
    }

    @Test
    void rightOutOfBounds() {
        final LoanWrapper l = mockWrapper(2);
        final MarketplaceFilterConditionImpl condition = new LoanAmountCondition(1, 1);
        assertThat(condition.test(l)).isFalse();
    }
}
