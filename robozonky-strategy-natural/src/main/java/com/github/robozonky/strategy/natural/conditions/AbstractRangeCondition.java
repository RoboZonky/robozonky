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

import java.util.Optional;

import com.github.robozonky.strategy.natural.InvestmentWrapper;
import com.github.robozonky.strategy.natural.LoanWrapper;
import com.github.robozonky.strategy.natural.ParticipationWrapper;
import com.github.robozonky.strategy.natural.Wrapper;

abstract class AbstractRangeCondition extends MarketplaceFilterConditionImpl implements MarketplaceFilterCondition {

    private final Number minInclusive, maxInclusive;

    protected AbstractRangeCondition(final Number minValueInclusive, final Number maxValueInclusive) {
        this.minInclusive = minValueInclusive;
        this.maxInclusive = maxValueInclusive;
    }

    protected Number retrieve(final Wrapper wrapper) {
        throw new UnsupportedOperationException(wrapper.toString());
    }

    protected Number retrieve(final InvestmentWrapper wrapper) {
        return retrieve((Wrapper) wrapper);
    }

    protected Number retrieve(final ParticipationWrapper wrapper) {
        return retrieve((Wrapper) wrapper);
    }

    protected Number retrieve(final LoanWrapper wrapper) {
        return retrieve((Wrapper) wrapper);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Range: <" + minInclusive + "; " + maxInclusive + ">.");
    }

    @Override
    public boolean test(final LoanWrapper item) {
        return new RangeCondition<LoanWrapper>(this::retrieve, minInclusive, maxInclusive).test(item);
    }

    @Override
    public boolean test(final InvestmentWrapper item) {
        return new RangeCondition<InvestmentWrapper>(this::retrieve, minInclusive, maxInclusive).test(item);
    }

    @Override
    public boolean test(final ParticipationWrapper item) {
        return new RangeCondition<ParticipationWrapper>(this::retrieve, minInclusive, maxInclusive).test(item);
    }
}
