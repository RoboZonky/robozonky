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

package com.github.robozonky.app.tenant;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.test.DateUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class AvailabilityImpl implements Availability {

    static final int MANDATORY_DELAY_IN_SECONDS = 5;
    private static final Logger LOGGER = LogManager.getLogger(AvailabilityImpl.class);
    private final ZonkyApiTokenSupplier zonkyApiTokenSupplier;
    private final AtomicReference<Tuple2<Instant, Long>> pause = new AtomicReference<>();

    public AvailabilityImpl(final ZonkyApiTokenSupplier zonkyTokenSupplier) {
        this.zonkyApiTokenSupplier = zonkyTokenSupplier;
    }

    @Override
    public Instant nextAvailabilityCheck() {
        if (zonkyApiTokenSupplier.isClosed()) {
            LOGGER.debug("Zonky OAuth2 token already closed, can not perform any more operations.");
            return Instant.MAX;
        } else if (isAvailable()) { // no waiting for anything
            return DateUtil.now();
        }
        final Tuple2<Instant, Long> paused = pause.get();
        final long retries = paused._2;
        // add 5 seconds of initial delay to give time to recover from HTTP 429 or whatever other problem there was
        final long secondsFromPauseToNextCheck = MANDATORY_DELAY_IN_SECONDS + (long) Math.pow(2, retries);
        return paused._1.plus(Duration.ofSeconds(secondsFromPauseToNextCheck));
    }

    @Override
    public boolean isAvailable() {
        return !zonkyApiTokenSupplier.isClosed() && pause.get() == null;
    }

    @Override
    public Optional<Instant> registerSuccess() {
        if (isAvailable()) {
            return Optional.empty();
        }
        final Tuple2<Instant, Long> paused = pause.getAndSet(null);
        LOGGER.info("Resumed after a forced pause.");
        return Optional.of(paused._1);
    }

    @Override
    public boolean registerException(final Exception ex) {
        if (isAvailable()) {
            pause.set(Tuple.of(DateUtil.now(), 0L));
            LOGGER.debug("Fault identified, forcing pause.", ex);
            // will go to console, no stack trace
            LOGGER.warn("Forcing a pause due to a potentially irrecoverable fault.");
            return true;
        } else {
            final Tuple2<Instant, Long> paused = pause.updateAndGet(f -> Tuple.of(f._1, f._2 + 1));
            LOGGER.debug("Forced pause in effect since {}, {} retries.", paused._1, paused._2, ex);
            return false;
        }
    }
}
