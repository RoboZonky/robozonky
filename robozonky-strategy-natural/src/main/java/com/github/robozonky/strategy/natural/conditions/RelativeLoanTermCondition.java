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

import com.github.robozonky.api.Ratio;
import com.github.robozonky.strategy.natural.wrappers.Wrapper;

public class RelativeLoanTermCondition extends AbstractRelativeRangeCondition {

    private RelativeLoanTermCondition(final RangeCondition<Ratio> condition) {
        super(condition);
    }

    public static RelativeLoanTermCondition lessThan(final Ratio threshold) {
        final RangeCondition<Ratio> c = RangeCondition.relativeLessThan(Wrapper::getRemainingTermInMonths,
                Wrapper::getOriginalTermInMonths,
                threshold);
        return new RelativeLoanTermCondition(c);
    }

    public static RelativeLoanTermCondition moreThan(final Ratio threshold) {
        final RangeCondition<Ratio> c = RangeCondition.relativeMoreThan(Wrapper::getRemainingTermInMonths,
                Wrapper::getOriginalTermInMonths,
                threshold);
        return new RelativeLoanTermCondition(c);
    }

    public static RelativeLoanTermCondition exact(final Ratio minimumThreshold, final Ratio maximumThreshold) {
        final RangeCondition<Ratio> c = RangeCondition.relativeExact(Wrapper::getRemainingTermInMonths,
                Wrapper::getOriginalTermInMonths,
                minimumThreshold, maximumThreshold);
        return new RelativeLoanTermCondition(c);
    }
}
