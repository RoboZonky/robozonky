/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy.natural;

import java.util.Optional;
import java.util.function.Function;

import com.github.triceo.robozonky.strategy.natural.conditions.RangeCondition;

abstract class AbstractRangeCondition extends MarketplaceFilterConditionImpl implements MarketplaceFilterCondition {

    private final RangeCondition<Wrapper> rangeCondition;

    protected AbstractRangeCondition(final Function<Wrapper, Number> targetAccessor, final Number minValueInclusive,
                                     final Number maxValueInclusive) {
        this.rangeCondition = new RangeCondition<>(targetAccessor, minValueInclusive, maxValueInclusive);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of(
                "Range: <" + rangeCondition.getMinInclusive() + "; " + rangeCondition.getMaxInclusive() + ">.");
    }

    @Override
    public boolean test(final Wrapper item) {
        return rangeCondition.test(item);
    }
}
