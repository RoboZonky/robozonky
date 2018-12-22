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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.vavr.control.Either;
import io.vavr.control.Try;

final class AsyncReloadableImpl<T> extends AbstractReloadableImpl<T> {

    private final AtomicReference<T> value = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<Void>> future = new AtomicReference<>();

    public AsyncReloadableImpl(final Supplier<T> supplier, final Consumer<T> runWhenReloaded) {
        super(supplier, runWhenReloaded);
    }

    public AsyncReloadableImpl(final Supplier<T> supplier, final Duration reloadAfter,
                               final Consumer<T> runWhenReloaded) {
        super(supplier, reloadAfter, runWhenReloaded);
    }

    public AsyncReloadableImpl(final Supplier<T> supplier) {
        super(supplier);
    }

    public AsyncReloadableImpl(final Supplier<T> supplier, final Duration reloadAfter) {
        super(supplier, reloadAfter);
    }

    @Override
    public synchronized Either<Throwable, T> get() {
        if (value.get() == null) { // force value retrieval and wait for it
            logger.debug("Fetching initial value synchronously on {}.", this);
            return Try.ofSupplier(getOperation())
                    .peek(v -> processRetrievedValue(v, value::set))
                    .toEither();
        }
        if (!needsReload()) { // return old value
            logger.trace("Not reloading {}.", this);
            return Either.right(value.get());
        }
        // trigger retrieval but return existing value
        final CompletableFuture<Void> currentFuture = this.future.updateAndGet(old -> {
            if (old == null || old.isDone()) {
                logger.trace("Starting async reload.");
                final Runnable asyncOperation = () -> Try.ofSupplier(getOperation())
                        .peek(v -> processRetrievedValue(v, value::set)) // set the value on success
                        .getOrElseGet(t -> {
                            logger.debug("Async reload failed.", t);
                            return null;
                        });
                return CompletableFuture.runAsync(asyncOperation, Scheduler.inBackground().getExecutor());
            } else {
                logger.trace("Reload already in progress.");
                return old;
            }
        });
        logger.trace("Operating Future: {}.", currentFuture);
        return Either.right(value.get());
    }
}
