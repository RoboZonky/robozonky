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
import java.util.Optional;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.strategy.natural.wrappers.Wrapper;

abstract class AbstractRangeCondition<T extends Number & Comparable<T>> extends MarketplaceFilterConditionImpl
        implements MarketplaceFilterCondition {

    protected static final Domain<Integer> AMOUNT_DOMAIN = new Domain<>(Integer.class, 0, null);
    protected static final Domain<BigDecimal> PRINCIPAL_DOMAIN = new Domain<>(BigDecimal.class, BigDecimal.ZERO, null);
    protected static final Domain<Ratio> RATE_DOMAIN = new Domain<>(Ratio.class, Ratio.ZERO, null);
    private static final int MAX_LOAN_TERM_IN_YEARS = 10;
    /**
     * Max loan term plus an estimated 1 year of possible delinquency on top.
     */
    protected static final Domain<Integer> LOAN_LIFE_IN_DAYS_DOMAIN = new Domain<>(Integer.class, 0,
            (MAX_LOAN_TERM_IN_YEARS + 1) * 365);
    protected static final Domain<Integer> LOAN_TERM_IN_MONTHS_DOMAIN = new Domain<>(Integer.class, 0,
            MAX_LOAN_TERM_IN_YEARS * 12);
    private final RangeCondition<T> rangeCondition;

    protected AbstractRangeCondition(final RangeCondition<T> condition, final boolean mayRequireRemoteRequests) {
        super(mayRequireRemoteRequests);
        this.rangeCondition = condition;
    }

    @Override
    public final Optional<String> getDescription() {
        return Optional.of(rangeCondition.toString());
    }

    @Override
    public final boolean test(final Wrapper<?> item) {
        return rangeCondition.test(item);
    }
}
