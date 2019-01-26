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
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.github.robozonky.internal.util.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class TimeBasedReload<T> implements ReloadDetection<T> {

    private static final Logger LOGGER = LogManager.getLogger(TimeBasedReload.class);

    private final AtomicReference<Instant> lastReloaded = new AtomicReference<>();
    private final AtomicReference<Duration> reloadAfter = new AtomicReference<>();
    private final Function<T, Duration> reloadFunction;

    public TimeBasedReload(final Function<T, Duration> reloadAfter) {
        this.reloadFunction = reloadAfter;
    }

    @Override
    public boolean getAsBoolean() {
        final Instant lastReloadedInstant = lastReloaded.get();
        return lastReloadedInstant == null ||
                lastReloadedInstant.plus(reloadAfter.get()).isBefore(DateUtil.now());
    }

    Optional<Duration> getReloadAfter() {
        return Optional.ofNullable(reloadAfter.get());
    }

    @Override
    public void markReloaded(final T newValue) {
        final Duration newReload = reloadFunction.apply(newValue);
        lastReloaded.set(DateUtil.now());
        reloadAfter.set(newReload);
        LOGGER.trace("Marked reloaded on {}, will be reloaded after {}.", this, newReload);
    }

    @Override
    public void forceReload() {
        lastReloaded.set(null);
        reloadAfter.set(null);
        LOGGER.trace("Forcing reload on {}.", this);
    }
}
