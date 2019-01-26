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

package com.github.robozonky.app.runtime;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.common.async.Refreshable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements {@link Lifecycle#waitUntilOnline()} by listening to {@link LivenessCheck} updates.
 */
class MainControl implements Refreshable.RefreshListener<String> {

    private static final Logger LOGGER = LogManager.getLogger(MainControl.class);
    private final AtomicReference<CountDownLatch> trigger = new AtomicReference<>(new CountDownLatch(1));
    private final AtomicReference<Tuple2<String, OffsetDateTime>> version =
            new AtomicReference<>(getVersionTuple(null));

    private static Tuple2<String, OffsetDateTime> getVersionTuple(final String version) {
        return Tuple.of(version, OffsetDateTime.now());
    }

    public void waitUntilTriggered() throws InterruptedException {
        LOGGER.trace("Waiting on {}.", this);
        trigger.get().await();
        LOGGER.trace("Wait over on {}.", this);
    }

    public Optional<String> getApiVersion() {
        return Optional.ofNullable(version.get()._1);
    }

    public OffsetDateTime getTimestamp() {
        return version.get()._2;
    }

    @Override
    public void valueSet(final String newApiVersion) { // becomes online, release
        version.set(getVersionTuple(newApiVersion));
        trigger.get().countDown();
        LOGGER.trace("Counted down on {}.", this);
    }

    @Override
    public void valueUnset(final String oldApiVersion) { // becomes offline, block
        version.set(getVersionTuple(null));
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
