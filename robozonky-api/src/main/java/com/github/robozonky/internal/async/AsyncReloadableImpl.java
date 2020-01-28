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

package com.github.robozonky.internal.async;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.vavr.control.Either;

final class AsyncReloadableImpl<T> extends AbstractReloadableImpl<T> {

    private final AtomicReference<T> value = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<Void>> future = new AtomicReference<>();

    public AsyncReloadableImpl(final Supplier<T> supplier, final UnaryOperator<T> reloader,
                               final Consumer<T> runWhenReloaded, final Set<ReloadListener<T>> listeners) {
        super(supplier, reloader, runWhenReloaded, listeners);
    }

    public AsyncReloadableImpl(final Supplier<T> supplier, final UnaryOperator<T> reloader,
                               final Consumer<T> runWhenReloaded, final Set<ReloadListener<T>> listeners,
                               final Function<T, Duration> reloadAfter) {
        super(supplier, reloader, runWhenReloaded, listeners, reloadAfter);
    }

    private synchronized void asyncReload() {
        logger.trace("Starting async reload.");
        try {
            var v = getOperation().apply(value.get());
            processRetrievedValue(v, value::set);
        } catch (Exception ex) {
            logger.debug("Async reload failed, operating with stale value.", ex);
        }
        logger.trace("Finished async reload.");
    }

    CompletableFuture<Void> refreshIfNotAlreadyRefreshing(final CompletableFuture<Void> old) {
        if (old == null || old.isDone()) {
            return CompletableFuture.runAsync(this::asyncReload);
        } else {
            logger.trace("Reload already in progress on {} with {}.", this, old);
            return old;
        }
    }

    @Override
    public Either<Throwable, T> get() {
        if (!hasValue()) { // force value retrieval and wait for it
            synchronized (this) {
                if (!hasValue()) { // double-checked locking to make sure the value is only ever loaded once
                    logger.debug("Fetching initial value synchronously on {}.", this);
                    try {
                        var v = getOperation().apply(null);
                        processRetrievedValue(v, value::set);
                        return Either.right(v);
                    } catch (Exception ex) {
                        return Either.left(ex);
                    }
                }
                // otherwise fall through to retrieve the current value
            }
        }
        if (needsReload()) { // trigger value retrieval on the background
            final CompletableFuture<Void> currentFuture = future.getAndUpdate(this::refreshIfNotAlreadyRefreshing);
            logger.debug("Retrieved potentially stale value on {}, while {}.", this, currentFuture);
        }
        // return the current value
        return Either.right(value.get());
    }

    @Override
    public boolean hasValue() {
        return value.get() != null;
    }
}
