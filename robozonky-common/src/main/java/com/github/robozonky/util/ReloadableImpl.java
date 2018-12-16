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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.robozonky.internal.util.DateUtil;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ReloadableImpl<T> implements Reloadable<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadableImpl.class);
    private static final Consumer DO_NOTHING = (x) -> {
        // do nothing
    };

    private final Supplier<T> supplier;
    private final Consumer<T> runWhenReloaded;
    private final ReloadDetecion needsReload;
    private final AtomicReference<T> value = new AtomicReference<>();

    public ReloadableImpl(final Supplier<T> supplier, final Consumer<T> runWhenReloaded) {
        this.supplier = supplier;
        this.runWhenReloaded = runWhenReloaded;
        this.needsReload = new ManualReload();
    }

    public ReloadableImpl(final Supplier<T> supplier, final Duration reloadAfter, final Consumer<T> runWhenReloaded) {
        this.supplier = supplier;
        this.runWhenReloaded = runWhenReloaded;
        this.needsReload = new TimeBasedReload(reloadAfter);
    }

    public ReloadableImpl(final Supplier<T> supplier) {
        this(supplier, DO_NOTHING);
    }

    public ReloadableImpl(final Supplier<T> supplier, final Duration reloadAfter) {
        this(supplier, reloadAfter, DO_NOTHING);
    }

    @Override
    public void clear() {
        needsReload.forceReload();
    }

    @Override
    public synchronized Either<Throwable, T> get() {
        if (!needsReload.getAsBoolean()) {
            LOGGER.trace("Not reloading {}.", this);
            return Either.right(value.get());
        }
        LOGGER.trace("Reloading {}.", this);
        return Try.ofSupplier(supplier).peek(v -> {
            LOGGER.trace("Supplier finished on {}.", this);
            value.set(v);
            needsReload.markReloaded();
            runWhenReloaded.accept(v);
            LOGGER.debug("Reloaded {}, new value is {}.", this, v);
        }).toEither();
    }

    private interface ReloadDetecion extends BooleanSupplier {

        void markReloaded();

        void forceReload();
    }

    private static final class ManualReload implements ReloadDetecion {

        private final AtomicBoolean needsReload = new AtomicBoolean(true);

        @Override
        public boolean getAsBoolean() {
            return needsReload.get();
        }

        @Override
        public void markReloaded() {
            needsReload.set(false);
            LOGGER.trace("Marked reloaded on {}.", this);
        }

        @Override
        public void forceReload() {
            needsReload.set(true);
            LOGGER.trace("Forcing reload on {}.", this);
        }
    }

    private static final class TimeBasedReload implements ReloadDetecion {

        private final AtomicReference<Instant> lastReloaded;
        private final Duration reloadAfter;

        public TimeBasedReload(Duration reloadAfter) {
            this.reloadAfter = reloadAfter;
            lastReloaded = new AtomicReference<>();
        }

        @Override
        public boolean getAsBoolean() {
            final Instant lastReloadedInstant = lastReloaded.get();
            return lastReloadedInstant == null ||
                    lastReloadedInstant.plus(reloadAfter).isBefore(DateUtil.now());
        }

        @Override
        public void markReloaded() {
            lastReloaded.set(DateUtil.now());
            LOGGER.trace("Marked reloaded on {}.", this);
        }

        @Override
        public void forceReload() {
            lastReloaded.set(null);
            LOGGER.trace("Forcing reload on {}.", this);
        }
    }
}
