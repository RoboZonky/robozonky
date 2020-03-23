/*
 * Copyright 2020 The RoboZonky Project
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

public final class ElapsedLoanTermCondition extends AbstractRangeCondition<Integer> {

    private ElapsedLoanTermCondition(final RangeCondition<Integer> condition) {
        super(condition);
    }

    private static int getElapsedTerm(final Wrapper<?> w) {
        return w.getOriginalTermInMonths() - w.getRemainingTermInMonths();
    }

    public static ElapsedLoanTermCondition lessThan(final int threshold) {
        final RangeCondition<Integer> c = RangeCondition.lessThan(ElapsedLoanTermCondition::getElapsedTerm,
                LOAN_TERM_DOMAIN, threshold);
        return new ElapsedLoanTermCondition(c);
    }

    public static ElapsedLoanTermCondition moreThan(final int threshold) {
        final RangeCondition<Integer> c = RangeCondition.moreThan(ElapsedLoanTermCondition::getElapsedTerm,
                LOAN_TERM_DOMAIN, threshold);
        return new ElapsedLoanTermCondition(c);
    }

    public static ElapsedLoanTermCondition exact(final int minimumThreshold, final int maximumThreshold) {
        final RangeCondition<Integer> c = RangeCondition.exact(ElapsedLoanTermCondition::getElapsedTerm,
                LOAN_TERM_DOMAIN, minimumThreshold,
                maximumThreshold);
        return new ElapsedLoanTermCondition(c);
    }
}
