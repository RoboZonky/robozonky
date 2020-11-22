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
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.test.DateUtil;

final class TimeBasedReload<T> implements ReloadDetection<T> {

    private static final Logger LOGGER = LogManager.getLogger(TimeBasedReload.class);

    private final AtomicReference<ZonedDateTime> lastReloadedRef = new AtomicReference<>();
    private final AtomicReference<Duration> reloadAfterRef = new AtomicReference<>();
    private final Function<T, Duration> reloadFunction;

    public TimeBasedReload(final Function<T, Duration> reloadAfter) {
        this.reloadFunction = reloadAfter;
    }

    @Override
    public boolean getAsBoolean() {
        var lastReloaded = this.lastReloadedRef.get();
        return lastReloaded == null ||
                lastReloaded.plus(reloadAfterRef.get())
                    .isBefore(DateUtil.zonedNow());
    }

    Optional<Duration> getReloadAfter() {
        return Optional.ofNullable(reloadAfterRef.get());
    }

    @Override
    public void markReloaded(final T newValue) {
        var newReload = reloadFunction.apply(newValue);
        lastReloadedRef.set(DateUtil.zonedNow());
        reloadAfterRef.set(newReload);
        LOGGER.trace("Marked reloaded on {}, will be reloaded after {}.", this, newReload);
    }

    @Override
    public void forceReload() {
        lastReloadedRef.set(null);
        reloadAfterRef.set(null);
        LOGGER.trace("Forcing reload on {}.", this);
    }
}
