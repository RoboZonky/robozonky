/*
 * Copyright 2021 The RoboZonky Project
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

import java.math.BigDecimal;

import com.github.robozonky.strategy.natural.wrappers.Wrapper;

public final class RemainingInterestCondition extends AbstractRangeCondition<BigDecimal> {

    private RemainingInterestCondition(final RangeCondition<BigDecimal> condition) {
        super(condition, false);
    }

    private static BigDecimal getRemainingInterest(final Wrapper<?> w) {
        return w.getRemainingInterest()
            .orElseThrow();
    }

    public static RemainingInterestCondition lessThan(final int threshold) {
        final RangeCondition<BigDecimal> c = RangeCondition.lessThan(RemainingInterestCondition::getRemainingInterest,
                PRINCIPAL_DOMAIN, BigDecimal.valueOf(threshold));
        return new RemainingInterestCondition(c);
    }

    public static RemainingInterestCondition moreThan(final int threshold) {
        final RangeCondition<BigDecimal> c = RangeCondition.moreThan(RemainingInterestCondition::getRemainingInterest,
                PRINCIPAL_DOMAIN, BigDecimal.valueOf(threshold));
        return new RemainingInterestCondition(c);
    }

    public static RemainingInterestCondition exact(final int minimumThreshold,
            final int maximumThreshold) {
        final RangeCondition<BigDecimal> c = RangeCondition.exact(RemainingInterestCondition::getRemainingInterest,
                PRINCIPAL_DOMAIN, BigDecimal.valueOf(minimumThreshold), BigDecimal.valueOf(maximumThreshold));
        return new RemainingInterestCondition(c);
    }
}
