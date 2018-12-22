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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractReloadableImpl<T> implements Reloadable<T> {

    private static final Consumer DO_NOTHING = (x) -> {
        // do nothing
    };
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final Supplier<T> operation;
    private final ReloadDetection needsReload;

    public AbstractReloadableImpl(final Supplier<T> supplier, final Consumer<T> runWhenReloaded) {
        this.operation = getOperation(supplier, runWhenReloaded);
        this.needsReload = new ManualReload();
    }

    public AbstractReloadableImpl(final Supplier<T> supplier, final Duration reloadAfter,
                                  final Consumer<T> runWhenReloaded) {
        this.operation = getOperation(supplier, runWhenReloaded);
        this.needsReload = new TimeBasedReload(reloadAfter);
    }

    public AbstractReloadableImpl(final Supplier<T> supplier) {
        this(supplier, DO_NOTHING);
    }

    public AbstractReloadableImpl(final Supplier<T> supplier, final Duration reloadAfter) {
        this(supplier, reloadAfter, DO_NOTHING);
    }

    private static <X> Supplier<X> getOperation(final Supplier<X> supplier, final Consumer<X> runWhenReloaded) {
        return () -> {
            final X value = supplier.get();
            runWhenReloaded.accept(value);  // first run finisher before setting the value, in case finisher fails
            return value;
        };
    }

    protected Supplier<T> getOperation() {
        return operation;
    }

    protected synchronized void markReloaded() {
        needsReload.markReloaded();
    }

    protected void processRetrievedValue(final T value, final Consumer<T> valueSetter) {
        logger.trace("Supplier finished on {}.", this);
        valueSetter.accept(value);
        markReloaded();
        logger.debug("Reloaded {}, new value is {}.", this, value);
    }

    protected synchronized boolean needsReload() {
        return needsReload.getAsBoolean();
    }

    @Override
    public synchronized void clear() {
        needsReload.forceReload();
    }
}
