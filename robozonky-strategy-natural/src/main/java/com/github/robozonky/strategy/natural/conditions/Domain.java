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

import java.util.Optional;
import java.util.function.Predicate;

final class Domain<T extends Number & Comparable<T>> implements Predicate<T> {

    private final Class<T> clz;
    private final T minimum;
    private final T maximum;

    public Domain(final Class<T> clz, final T minimum, final T maximum) {
        this.clz = clz;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public Optional<T> getMinimum() {
        return Optional.ofNullable(minimum);
    }

    public Optional<T> getMaximum() {
        return Optional.ofNullable(maximum);
    }

    @Override
    public final String toString() {
        return "Domain{" +
                "clz=" + clz +
                ", minimum=" + minimum +
                ", maximum=" + maximum +
                '}';
    }

    @Override
    public final boolean test(final T t) {
        if (minimum == null) {
            return t.compareTo(maximum) <= 0;
        } else if (maximum == null) {
            return t.compareTo(minimum) >= 0;
        } else {
            return t.compareTo(minimum) >= 0 && t.compareTo(maximum) <= 0;
        }
    }
}
