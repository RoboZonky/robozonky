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

public final class CurrentDelinquencyCondition extends AbstractRangeCondition<Integer> {

    private CurrentDelinquencyCondition(final RangeCondition<Integer> condition) {
        super(condition);
    }

    private static int getDelinquentDays(Wrapper<?> w) {
        return w.getCurrentDpd()
            .orElse(0);
    }

    public static CurrentDelinquencyCondition lessThan(final int threshold) {
        final RangeCondition<Integer> c = RangeCondition.lessThan(CurrentDelinquencyCondition::getDelinquentDays,
                LOAN_TERM_DOMAIN, threshold);
        return new CurrentDelinquencyCondition(c);
    }

    public static CurrentDelinquencyCondition moreThan(final int threshold) {
        final RangeCondition<Integer> c = RangeCondition.moreThan(CurrentDelinquencyCondition::getDelinquentDays,
                LOAN_TERM_DOMAIN, threshold);
        return new CurrentDelinquencyCondition(c);
    }

    public static CurrentDelinquencyCondition exact(final int minimumThreshold, final int maximumThreshold) {
        final RangeCondition<Integer> c = RangeCondition.exact(CurrentDelinquencyCondition::getDelinquentDays,
                LOAN_TERM_DOMAIN, minimumThreshold, maximumThreshold);
        return new CurrentDelinquencyCondition(c);
    }
}
