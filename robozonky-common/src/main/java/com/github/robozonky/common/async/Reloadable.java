/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.common.async;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.vavr.control.Either;

public interface Reloadable<T> {

    static <V> ReloadableBuilder<V> with(final Supplier<V> supplier) {
        return new ReloadableBuilder<>(supplier);
    }

    static <V> Reloadable<V> of(final Supplier<V> supplier, final Consumer<V> runWhenLoaded) {
        return with(supplier)
                .finishWith(runWhenLoaded)
                .build();
    }

    static <V> Reloadable<V> of(final Supplier<V> supplier) {
        return with(supplier)
                .build();
    }

    static <V> Reloadable<V> of(final Supplier<V> supplier, final Duration reloadAfter,
                                final Consumer<V> runWhenLoaded) {
        return with(supplier)
                .reloadAfter(reloadAfter)
                .finishWith(runWhenLoaded)
                .build();
    }

    static <V> Reloadable<V> of(final Supplier<V> supplier, final Duration reloadAfter) {
        return with(supplier)
                .reloadAfter(reloadAfter)
                .build();
    }

    void clear();

    Either<Throwable, T> get();
}