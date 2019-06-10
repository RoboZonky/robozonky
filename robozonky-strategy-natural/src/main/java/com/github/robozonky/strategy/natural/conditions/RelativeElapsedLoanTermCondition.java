/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.Ratio;
import com.github.robozonky.strategy.natural.Wrapper;

public class RelativeElapsedLoanTermCondition extends AbstractRelativeRangeCondition {

    private RelativeElapsedLoanTermCondition(final RangeCondition<Ratio> condition) {
        super(condition);
    }

    private static int getElapsedTerm(final Wrapper<?> w) {
        return w.getOriginalTermInMonths() - w.getRemainingTermInMonths();
    }

    public static RelativeElapsedLoanTermCondition lessThan(final Ratio threshold) {
        final RangeCondition<Ratio> c = RangeCondition.relativeLessThan(RelativeElapsedLoanTermCondition::getElapsedTerm,
                                                                        Wrapper::getRemainingTermInMonths,
                                                                        threshold);
        return new RelativeElapsedLoanTermCondition(c);
    }

    public static RelativeElapsedLoanTermCondition moreThan(final Ratio threshold) {
        final RangeCondition<Ratio> c = RangeCondition.relativeMoreThan(RelativeElapsedLoanTermCondition::getElapsedTerm,
                                                                        Wrapper::getRemainingTermInMonths,
                                                                        threshold);
        return new RelativeElapsedLoanTermCondition(c);
    }

    public static RelativeElapsedLoanTermCondition exact(final Ratio minimumThreshold, final Ratio maximumThreshold) {
        final RangeCondition<Ratio> c = RangeCondition.relativeExact(RelativeElapsedLoanTermCondition::getElapsedTerm,
                                                                     Wrapper::getRemainingTermInMonths,
                                                                     minimumThreshold, maximumThreshold);
        return new RelativeElapsedLoanTermCondition(c);
    }
}
