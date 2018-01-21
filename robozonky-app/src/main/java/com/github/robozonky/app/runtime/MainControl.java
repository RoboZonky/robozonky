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

package com.github.robozonky.app.runtime;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.util.Refreshable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link Lifecycle#waitUntilOnline()} by listening to {@link LivenessCheck} updates.
 */
class MainControl implements Refreshable.RefreshListener<ApiVersion> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainControl.class);
    private final AtomicReference<CountDownLatch> trigger = new AtomicReference<>(new CountDownLatch(1));
    private final AtomicReference<ApiVersion> version = new AtomicReference<>();

    public void waitUntilTriggered() throws InterruptedException {
        LOGGER.trace("Waiting on {}.", this);
        trigger.get().await();
        LOGGER.trace("Wait over on {}.", this);
    }

    public Optional<ApiVersion> getApiVersion() {
        return Optional.ofNullable(version.get());
    }

    @Override
    public void valueSet(final ApiVersion newValue) { // becomes online, release
        version.set(newValue);
        trigger.get().countDown();
        LOGGER.trace("Counted down on {}.", this);
    }

    @Override
    public void valueUnset(final ApiVersion oldValue) { // becomes offline, block
        version.set(null);
        trigger.updateAndGet(currentTrigger -> {
            if (currentTrigger.getCount() == 0) { // already triggered, can set new trigger
                LOGGER.trace("Countdown restarted on {}.", this);
                return new CountDownLatch(1);
            } else {
                LOGGER.trace("Countdown untouched on {}.", this);
                return currentTrigger;
            }
        });
    }
}
