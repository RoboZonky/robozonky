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

public class ElapsedLoanTermCondition extends AbstractRangeCondition {

    private static final int MIN_TERM = 0, MAX_TERM = 84;

    private static void assertIsInRange(final int months) {
        if ((months < ElapsedLoanTermCondition.MIN_TERM) || (months > ElapsedLoanTermCondition.MAX_TERM)) {
            throw new IllegalArgumentException(
                    "Loan term must be in range of <" + ElapsedLoanTermCondition.MIN_TERM + "; "
                            + ElapsedLoanTermCondition.MAX_TERM + ">, but was " + months);
        }
    }

    public ElapsedLoanTermCondition(final int fromInclusive, final int toInclusive) {
        super(w -> w.getOriginalTermInMonths() - w.getRemainingTermInMonths(), fromInclusive, toInclusive);
        ElapsedLoanTermCondition.assertIsInRange(fromInclusive);
        ElapsedLoanTermCondition.assertIsInRange(toInclusive);
    }

    public ElapsedLoanTermCondition(final int fromInclusive) {
        this(fromInclusive, ElapsedLoanTermCondition.MAX_TERM);
    }
}
