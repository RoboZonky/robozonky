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

import com.github.robozonky.api.Ratio;
import com.github.robozonky.internal.util.BigDecimalCalculator;
import com.github.robozonky.strategy.natural.wrappers.Wrapper;

public class RelativeDiscountCondition extends AbstractRelativeRangeCondition {

    private RelativeDiscountCondition(final RangeCondition<Ratio> condition) {
        super(condition, true);
    }

    private static BigDecimal getDiscount(final Wrapper<?> w) {
        BigDecimal price = w.getSellPrice()
            .orElse(w.getRemainingPrincipal());
        BigDecimal principal = w.getRemainingPrincipal();
        return BigDecimalCalculator.minus(principal, price);
    }

    public static RelativeDiscountCondition lessThan(final Ratio threshold) {
        final RangeCondition<Ratio> c = RangeCondition.relativeLessThan(RelativeDiscountCondition::getDiscount,
                Wrapper::getRemainingPrincipal, threshold);
        return new RelativeDiscountCondition(c);
    }

    public static RelativeDiscountCondition moreThan(final Ratio threshold) {
        final RangeCondition<Ratio> c = RangeCondition.relativeMoreThan(RelativeDiscountCondition::getDiscount,
                Wrapper::getRemainingPrincipal, threshold);
        return new RelativeDiscountCondition(c);
    }

    public static RelativeDiscountCondition exact(final Ratio minimumThreshold, final Ratio maximumThreshold) {
        final RangeCondition<Ratio> c = RangeCondition.relativeExact(RelativeDiscountCondition::getDiscount,
                Wrapper::getRemainingPrincipal, minimumThreshold, maximumThreshold);
        return new RelativeDiscountCondition(c);
    }
}
