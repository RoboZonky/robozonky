/*
 * Copyright 2018 The RoboZonky Project
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

import com.github.robozonky.strategy.natural.Wrapper;

public class ExitStrategyLoanTermCondition extends AbstractRangeCondition {

    private static final int MIN_TERM = 0, MAX_TERM = 84;

    private static void assertIsInRange(final long months) {
        if ((months < ExitStrategyLoanTermCondition.MIN_TERM) || (months > ExitStrategyLoanTermCondition.MAX_TERM)) {
            throw new IllegalArgumentException(
                    "Loan term must be in range of <" + ExitStrategyLoanTermCondition.MIN_TERM + "; "
                            + ExitStrategyLoanTermCondition.MAX_TERM + ">, but was " + months);
        }
    }

    public ExitStrategyLoanTermCondition(final long fromInclusive, final long toInclusive) {
        super(fromInclusive, toInclusive);
        ExitStrategyLoanTermCondition.assertIsInRange(fromInclusive);
        ExitStrategyLoanTermCondition.assertIsInRange(toInclusive);
    }

    public ExitStrategyLoanTermCondition(final long fromInclusive) {
        this(fromInclusive, ExitStrategyLoanTermCondition.MAX_TERM);
    }

    @Override
    protected Number retrieve(final Wrapper wrapper) {
        return wrapper.getOriginalTermInMonths();
    }
}
