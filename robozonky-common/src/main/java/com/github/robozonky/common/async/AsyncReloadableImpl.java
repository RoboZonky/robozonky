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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AsyncReloadableImpl<T> implements Reloadable<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncReloadableImpl.class);
    private static final Consumer DO_NOTHING = (x) -> {
        // do nothing
    };

    private final Supplier<T> operation;
    private final ReloadDetection needsReload;
    private final AtomicReference<T> value = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<T>> future = new AtomicReference<>();

    public AsyncReloadableImpl(final Supplier<T> supplier, final Consumer<T> runWhenReloaded) {
        this.operation = getOperation(supplier, runWhenReloaded);
        this.needsReload = new ManualReload();
    }

    public AsyncReloadableImpl(final Supplier<T> supplier, final Duration reloadAfter,
                               final Consumer<T> runWhenReloaded) {
        this.operation = getOperation(supplier, runWhenReloaded);
        this.needsReload = new TimeBasedReload(reloadAfter);
    }

    public AsyncReloadableImpl(final Supplier<T> supplier) {
        this(supplier, DO_NOTHING);
    }

    public AsyncReloadableImpl(final Supplier<T> supplier, final Duration reloadAfter) {
        this(supplier, reloadAfter, DO_NOTHING);
    }

    private static <X> Supplier<X> getOperation(final Supplier<X> supplier, final Consumer<X> runWhenReloaded) {
        return () -> {
            final X value = supplier.get();
            runWhenReloaded.accept(value);  // first run finisher before setting the value, in case finisher fails
            return value;
        };
    }

    @Override
    public synchronized void clear() {
        needsReload.forceReload();
    }

    @Override
    public synchronized Either<Throwable, T> get() {
        if (value.get() == null) { // force value retrieval and wait for it
            LOGGER.debug("Fetching initial value synchronously on {}.", this);
            return Try.ofSupplier(operation)
                    .peek(v -> {
                        LOGGER.trace("Supplier finished on {}.", this);
                        value.set(v);
                        needsReload.markReloaded();
                        LOGGER.debug("Reloaded {}, new value is {}.", this, v);
                    })
                    .toEither();
        }
        if (!needsReload.getAsBoolean()) { // return old value
            LOGGER.trace("Not reloading {}.", this);
            return Either.right(value.get());
        }
        // trigger retrieval but return old value
        final CompletableFuture<T> currentFuture = this.future.updateAndGet(old -> {
            if (old == null || old.isDone()) {
                LOGGER.trace("Starting async reload.");
                final Supplier<T> asyncOperation = () -> Try.ofSupplier(operation)
                        .peek(v -> {
                            LOGGER.trace("Supplier finished on {}.", this);
                            value.set(v);
                            needsReload.markReloaded();
                            LOGGER.debug("Reloaded {}, new value is {}.", this, v);
                        })
                        .getOrElseGet(t -> {
                            LOGGER.debug("Async reload failed.", t);
                            return null;
                        });
                return CompletableFuture.supplyAsync(asyncOperation, Scheduler.inBackground().getExecutor());
            } else {
                LOGGER.trace("Reload already in progress.");
                return old;
            }
        });
        LOGGER.trace("Operating Future: {}.", currentFuture);
        return Either.right(value.get());
    }
}
