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

import java.math.BigDecimal;
import java.util.Optional;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.strategy.natural.Wrapper;

abstract class AbstractRangeCondition<T extends Number & Comparable<T>> extends MarketplaceFilterConditionImpl
        implements MarketplaceFilterCondition {

    protected static final Domain<Integer> LOAN_TERM_DOMAIN = new Domain<>(Integer.class, 0, 84);
    protected static final Domain<Integer> AMOUNT_DOMAIN = new Domain<>(Integer.class, 0, null);
    protected static final Domain<BigDecimal> PRINCIPAL_DOMAIN = new Domain<>(BigDecimal.class, BigDecimal.ZERO, null);
    protected static final Domain<Ratio> RATE_DOMAIN = new Domain<>(Ratio.class, Ratio.ZERO, null);
    private final RangeCondition<T> rangeCondition;

    protected AbstractRangeCondition(final RangeCondition<T> condition) {
        this.rangeCondition = condition;
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of(rangeCondition.toString());
    }

    @Override
    public boolean test(final Wrapper<?> item) {
        return rangeCondition.test(item);
    }
}
