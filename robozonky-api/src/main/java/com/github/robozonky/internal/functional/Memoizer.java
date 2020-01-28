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

package com.github.robozonky.internal.functional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Memoizer<In, Out> {

    private final Map<In, Out> memoizationCache = new ConcurrentHashMap<>(0);

    public static <T, U> Function<T, U> memoize(final Function<T, U> function) {
        return new Memoizer<T, U>().doMemoize(function);
    }

    public static <Out> Supplier<Out> memoize(final Supplier<Out> supplier) {
        Function<Boolean, Out> memoized = memoize(input -> supplier.get());
        return () -> memoized.apply(true);
    }

    private Function<In, Out> doMemoize(final Function<In, Out> function) {
        return input -> memoizationCache.computeIfAbsent(input, function);
    }

}
