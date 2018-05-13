/*
 * Copyright 2017 The RoboZonky Project
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

import com.github.robozonky.strategy.natural.InvestmentWrapper;
import com.github.robozonky.strategy.natural.LoanWrapper;
import com.github.robozonky.strategy.natural.ParticipationWrapper;
import com.github.robozonky.strategy.natural.Util;
import com.github.robozonky.strategy.natural.Wrapper;

abstract class AbstractRelativeRangeCondition extends MarketplaceFilterConditionImpl
        implements MarketplaceFilterCondition {

    private final Number minInclusive, maxInclusive;

    protected AbstractRelativeRangeCondition(final Number minValueInclusive, final Number maxValueInclusive) {
        assertIsInRange(minValueInclusive);
        assertIsInRange(maxValueInclusive);
        this.minInclusive = minValueInclusive;
        this.maxInclusive = maxValueInclusive;
    }

    private static BigDecimal getActualValue(final Number sum, final Number percentage) {
        final BigDecimal s = Util.toBigDecimal(sum);
        final BigDecimal p = Util.toBigDecimal(percentage);
        return s.multiply(p).scaleByPowerOfTen(-2);
    }

    private void assertIsInRange(final Number percentage) {
        final double num = Util.toBigDecimal(percentage).doubleValue();
        if (num < 0 || num > 100) {
            throw new IllegalArgumentException("Relative value must be in range of <0; 100> %, but was " + percentage);
        }
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Relative range: <" + minInclusive + "; " + maxInclusive + "> %.");
    }

    protected Number accessSum(final Wrapper wrapper) {
        throw new UnsupportedOperationException();
    }

    protected Number accessSum(final InvestmentWrapper wrapper) {
        return accessSum((Wrapper) wrapper);
    }

    protected Number accessSum(final ParticipationWrapper wrapper) {
        return accessSum((Wrapper) wrapper);
    }

    protected Number accessSum(final LoanWrapper wrapper) {
        return accessSum((Wrapper) wrapper);
    }

    protected Number accessTarget(final Wrapper wrapper) {
        throw new UnsupportedOperationException();
    }

    protected Number accessTarget(final InvestmentWrapper wrapper) {
        return accessTarget((Wrapper) wrapper);
    }

    protected Number accessTarget(final ParticipationWrapper wrapper) {
        return accessTarget((Wrapper) wrapper);
    }

    protected Number accessTarget(final LoanWrapper wrapper) {
        return accessTarget((Wrapper) wrapper);
    }

    @Override
    public boolean test(final InvestmentWrapper item) {
        final BigDecimal realMinInclusive = getActualValue(accessSum(item), minInclusive);
        final BigDecimal realMaxInclusive = getActualValue(accessSum(item), maxInclusive);
        return new RangeCondition<InvestmentWrapper>(this::accessTarget, realMinInclusive,
                                                     realMaxInclusive).test(item);
    }

    @Override
    public boolean test(final ParticipationWrapper item) {
        final BigDecimal realMinInclusive = getActualValue(accessSum(item), minInclusive);
        final BigDecimal realMaxInclusive = getActualValue(accessSum(item), maxInclusive);
        return new RangeCondition<ParticipationWrapper>(this::accessTarget, realMinInclusive,
                                                        realMaxInclusive).test(item);
    }

    @Override
    public boolean test(final LoanWrapper item) {
        final BigDecimal realMinInclusive = getActualValue(accessSum(item), minInclusive);
        final BigDecimal realMaxInclusive = getActualValue(accessSum(item), maxInclusive);
        return new RangeCondition<LoanWrapper>(this::accessTarget, realMinInclusive, realMaxInclusive).test(item);
    }
}
