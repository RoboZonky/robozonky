/*
 * Copyright 2019 The RoboZonky Project
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class AbstractReloadableImpl<T> implements Reloadable<T> {

    protected final Logger logger = LogManager.getLogger(getClass());
    private final UnaryOperator<T> operation;
    private final ReloadDetection<T> needsReload;

    public AbstractReloadableImpl(final Supplier<T> supplier, final UnaryOperator<T> reloader,
                                  final Consumer<T> runWhenReloaded) {
        this.operation = getOperation(supplier, reloader, runWhenReloaded);
        this.needsReload = new ManualReload<>();
    }

    public AbstractReloadableImpl(final Supplier<T> supplier, final UnaryOperator<T> reloader,
                                  final Consumer<T> runWhenReloaded, final Function<T, Duration> reloadAfter) {
        this.operation = getOperation(supplier, reloader, runWhenReloaded);
        this.needsReload = new TimeBasedReload<>(reloadAfter);
    }

    private <X> UnaryOperator<X> getOperation(final Supplier<X> supplier, final UnaryOperator<X> reloader,
                                              final Consumer<X> runWhenReloaded) {
        return original -> {
            logger.trace("Running operation on {}, previous value given: {}.", this, original);
            final X value = original == null ? supplier.get() : reloader.apply(original);
            runWhenReloaded.accept(value);  // first run finisher before setting the value, in case finisher fails
            logger.trace("Operation finished on {}.", this);
            return value;
        };
    }

    protected UnaryOperator<T> getOperation() {
        return operation;
    }

    protected void processRetrievedValue(final T value, final Consumer<T> valueSetter) {
        logger.trace("Supplier finished on {}.", this);
        valueSetter.accept(value);
        needsReload.markReloaded(value);
        logger.debug("Reloaded {}, new value is {}.", this, value);
    }

    protected boolean needsReload() {
        return needsReload.getAsBoolean();
    }

    @Override
    public void clear() {
        needsReload.forceReload();
    }
}
