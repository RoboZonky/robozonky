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

import java.util.function.Predicate;

import com.github.robozonky.strategy.natural.wrappers.Wrapper;

class AbstractBooleanCondition extends MarketplaceFilterConditionImpl {

    private final Predicate<Wrapper<?>> predicate;
    protected final boolean expected;

    protected AbstractBooleanCondition(final Predicate<Wrapper<?>> predicate, final boolean expected,
            final boolean mayRequireRemoteRequest) {
        super(mayRequireRemoteRequest);
        this.predicate = predicate;
        this.expected = expected;
    }

    @Override
    public boolean test(final Wrapper<?> loan) {
        final boolean actual = predicate.test(loan);
        return actual == expected;
    }
}
