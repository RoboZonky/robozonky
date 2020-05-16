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

import static com.github.robozonky.internal.util.BigDecimalCalculator.minus;

import java.math.BigDecimal;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.strategy.natural.wrappers.Wrapper;

public class RelativeProfitCondition extends AbstractRelativeRangeCondition {

    private RelativeProfitCondition(final RangeCondition<Ratio> condition) {
        super(condition);
    }

    private static BigDecimal getProfit(final Wrapper<?> w) {
        BigDecimal income = w.getReturns()
            .orElse(BigDecimal.ZERO);
        BigDecimal saleFee = w.getSellFee()
            .orElse(BigDecimal.ZERO);
        BigDecimal result = minus(minus(income, saleFee), getOriginalPrice(w));
        return result;
    }

    private static BigDecimal getOriginalPrice(final Wrapper<?> w) {
        // Return original amount in case this is called during investing, when checking sell filters.
        return w.getOriginalPurchasePrice()
            .orElseGet(() -> BigDecimal.valueOf(w.getOriginalAmount()));
    }

    public static RelativeProfitCondition lessThan(final Ratio threshold) {
        final RangeCondition<Ratio> c = RangeCondition.relativeLessThan(RelativeProfitCondition::getProfit,
                RelativeProfitCondition::getOriginalPrice,
                threshold);
        return new RelativeProfitCondition(c);
    }

    public static RelativeProfitCondition moreThan(final Ratio threshold) {
        final RangeCondition<Ratio> c = RangeCondition.relativeMoreThan(RelativeProfitCondition::getProfit,
                RelativeProfitCondition::getOriginalPrice,
                threshold);
        return new RelativeProfitCondition(c);
    }

    public static RelativeProfitCondition exact(final Ratio minimumThreshold, final Ratio maximumThreshold) {
        final RangeCondition<Ratio> c = RangeCondition.relativeExact(RelativeProfitCondition::getProfit,
                RelativeProfitCondition::getOriginalPrice,
                minimumThreshold, maximumThreshold);
        return new RelativeProfitCondition(c);
    }
}
