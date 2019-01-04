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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.vavr.control.Either;
import io.vavr.control.Try;

final class ReloadableImpl<T> extends AbstractReloadableImpl<T> {

    private final AtomicReference<T> value = new AtomicReference<>();

    public ReloadableImpl(final Supplier<T> supplier, final Consumer<T> runWhenReloaded) {
        super(supplier, runWhenReloaded);
    }

    public ReloadableImpl(final Supplier<T> supplier, final Duration reloadAfter, final Consumer<T> runWhenReloaded) {
        super(supplier, reloadAfter, runWhenReloaded);
    }

    public ReloadableImpl(final Supplier<T> supplier) {
        super(supplier);
    }

    public ReloadableImpl(final Supplier<T> supplier, final Duration reloadAfter) {
        super(supplier, reloadAfter);
    }

    @Override
    public Either<Throwable, T> get() {
        if (needsReload()) { // double-checked locking to make sure the reload only happens once
            synchronized (this) {
                if (needsReload()) {
                    logger.trace("Reloading {}.", this);
                    return Try.ofSupplier(getOperation())
                            .peek(v -> processRetrievedValue(v, value::set))
                            .toEither();
                }
                // otherwise fall through to retrieve the value
            }
        }
        logger.trace("Not reloading {}.", this);
        return Either.right(value.get());
    }
}
