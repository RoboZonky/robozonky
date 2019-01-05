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
import java.util.function.Supplier;

import io.vavr.control.Either;

/**
 * Allows the user to have a variable which reloads from a remote source on-demand.
 * @param <T> Type of the variable.
 */
public interface Reloadable<T> {

    /**
     * Start a builder chain with the supplier as the operation used to reload the variable.
     * @param supplier Operation that loads the variable.
     * @param <V> Type of the variable.
     * @return The builder.
     */
    static <V> ReloadableBuilder<V> with(final Supplier<V> supplier) {
        return new ReloadableBuilder<>(supplier);
    }

    /**
     * Marks the {@link Reloadable} for reload. Next time {@link #get()} is called, the operation will be re-attempted.
     */
    void clear();

    /**
     * Get the previous value of the operation (see {@link #with(Supplier)}, potentially executing it anew. The behavior
     * of this method is greatly influenced by {@link ReloadableBuilder}, most importantly its methods
     * {@link ReloadableBuilder#async()} and {@link ReloadableBuilder#reloadAfter(Duration)}.
     * @return Result of the last call of the operation.
     */
    Either<Throwable, T> get();

    boolean hasValue();
}
