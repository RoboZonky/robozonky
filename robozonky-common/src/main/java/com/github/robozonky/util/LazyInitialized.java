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

package com.github.robozonky.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class LazyInitialized<T> implements Supplier<T> {

    private final Supplier<T> initializer;
    private final AtomicReference<T> value = new AtomicReference<>();

    private LazyInitialized(final Supplier<T> initializer) {
        this.initializer = initializer;
    }

    public static <X> LazyInitialized<X> create(final Supplier<X> initializer) {
        return new LazyInitialized<>(initializer);
    }

    public void reset() {
        value.set(null);
    }

    @Override
    public T get() {
        return value.updateAndGet(old -> {
            if (old != null) {
                return old;
            }
            return initializer.get();
        });
    }
}
