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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class ManualReload<T> implements ReloadDetection<T> {

    private static final Logger LOGGER = LogManager.getLogger(ManualReload.class);

    private final AtomicBoolean needsReload = new AtomicBoolean(true);

    @Override
    public boolean getAsBoolean() {
        return needsReload.get();
    }

    @Override
    public void markReloaded(final T newValue) {
        needsReload.set(false);
        LOGGER.trace("Marked reloaded on {}.", this);
    }

    @Override
    public void forceReload() {
        needsReload.set(true);
        LOGGER.trace("Forcing reload on {}.", this);
    }
}
