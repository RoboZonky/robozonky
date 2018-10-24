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

package com.github.robozonky.internal.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class LazyInitialized<T> implements Supplier<T>,
                                                 AutoCloseable {

    private final Supplier<T> initializer;
    private final Consumer<T> destructor;
    private final AtomicReference<T> value = new AtomicReference<>();

    private LazyInitialized(final Supplier<T> initializer, final Consumer<T> destructor) {
        this.initializer = initializer;
        this.destructor = destructor;
    }

    public static <X> LazyInitialized<X> create(final Supplier<X> initializer) {
        return create(initializer, null);
    }

    public static <X> LazyInitialized<X> create(final Supplier<X> initializer, final Consumer<X> destructor) {
        return new LazyInitialized<>(initializer, destructor);
    }

    public void reset() {
        final T val = value.getAndSet(null);
        if (destructor == null || val == null) {
            return;
        }
        destructor.accept(val);
    }

    @Override
    public T get() {
        // first read the value without locking, in case it already exists
        final T result = value.get();
        if (result != null) {
            return result;
        }
        // when we know it doesn't exist, lock and set it
        synchronized (this) {
            return value.updateAndGet(old -> {
                if (old != null) {
                    return old;
                }
                return initializer.get();
            });
        }
    }

    @Override
    public void close() {
        reset();
    }
}
