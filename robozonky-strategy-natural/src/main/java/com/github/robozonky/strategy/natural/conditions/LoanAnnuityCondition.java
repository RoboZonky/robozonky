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

import com.github.robozonky.strategy.natural.Wrapper;

public final class LoanAnnuityCondition extends AbstractRangeCondition<Integer> {

    private LoanAnnuityCondition(final NewRangeCondition<Integer> condition) {
        super(condition);
    }

    public static LoanAnnuityCondition lessThan(final int threshold) {
        final NewRangeCondition<Integer> c = NewRangeCondition.lessThan(Wrapper::getOriginalAnnuity, AMOUNT_DOMAIN,
                                                                        threshold);
        return new LoanAnnuityCondition(c);
    }

    public static LoanAnnuityCondition moreThan(final int threshold) {
        final NewRangeCondition<Integer> c = NewRangeCondition.moreThan(Wrapper::getOriginalAnnuity, AMOUNT_DOMAIN,
                                                                        threshold);
        return new LoanAnnuityCondition(c);
    }

    public static LoanAnnuityCondition exact(final int minimumThreshold, final int maximumThreshold) {
        final NewRangeCondition<Integer> c = NewRangeCondition.exact(Wrapper::getOriginalAnnuity, AMOUNT_DOMAIN,
                                                                     minimumThreshold, maximumThreshold);
        return new LoanAnnuityCondition(c);
    }
}
