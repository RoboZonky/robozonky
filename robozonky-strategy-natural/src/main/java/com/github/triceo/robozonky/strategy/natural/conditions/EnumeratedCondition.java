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

package com.github.triceo.robozonky.strategy.natural.conditions;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

public class EnumeratedCondition<T, S> implements Condition<T> {

    private final Function<T, S> targetAccessor;
    private final Set<S> allowedValues;

    public EnumeratedCondition(final Function<T, S> targetAccessor, final Collection<S> allowedValues) {
        this.targetAccessor = targetAccessor;
        this.allowedValues = new LinkedHashSet<>(allowedValues);
    }

    public Set<S> getAllowedValues() {
        return allowedValues;
    }

    @Override
    public boolean test(final T item) {
        final S match = targetAccessor.apply(item);
        return allowedValues.contains(match);
    }
}
