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

public class LoanTermCondition extends AbstractRangeCondition {

    private static final int MIN_TERM = 0, MAX_TERM = 84;

    private static void assertIsInRange(final int months) {
        if ((months < LoanTermCondition.MIN_TERM) || (months > LoanTermCondition.MAX_TERM)) {
            throw new IllegalArgumentException("Loan terms must be in range of <" + LoanTermCondition.MIN_TERM + "; "
                    + LoanTermCondition.MAX_TERM + ">.");
        }
    }

    public LoanTermCondition(final int fromInclusive, final int toInclusive) {
        super(Loan::getTermInMonths, fromInclusive, toInclusive);
        LoanTermCondition.assertIsInRange(fromInclusive);
        LoanTermCondition.assertIsInRange(toInclusive);
    }

    public LoanTermCondition(final int fromInclusive) {
        this(fromInclusive, LoanTermCondition.MAX_TERM);
    }

}
