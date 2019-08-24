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
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.internal.remote.RequestCounter;
import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.test.DateUtil;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class AvailabilityImpl implements Availability {

    private static final Logger LOGGER = LogManager.getLogger(AvailabilityImpl.class);

    private final ZonkyApiTokenSupplier zonkyApiTokenSupplier;
    private final AtomicReference<Tuple3<Instant, Long, Long>> pause = new AtomicReference<>();
    private final RequestCounter counter;

    public AvailabilityImpl(final ZonkyApiTokenSupplier zonkyTokenSupplier, final RequestCounter counter) {
        this.zonkyApiTokenSupplier = zonkyTokenSupplier;
        this.counter = counter;
    }

    @Override
    public Instant nextAvailabilityCheck() {
        if (zonkyApiTokenSupplier.isClosed()) {
            LOGGER.debug("Zonky OAuth2 token already closed, can not perform any more operations.");
            return Instant.MAX;
        } else if (isAvailable()) { // no waiting for anything
            return DateUtil.now();
        }
        final Tuple3<Instant, Long, Long> paused = pause.get();
        final long retries = paused._2;
        final long secondsFromPauseToNextCheck = (long) Math.pow(2, retries);
        return paused._1.plus(Duration.ofSeconds(secondsFromPauseToNextCheck));
    }

    @Override
    public boolean isAvailable() {
        return !zonkyApiTokenSupplier.isClosed() && pause.get() == null;
    }

    @Override
    public boolean registerSuccess() {
        if (isAvailable()) {
            return false;
        }
        final Tuple3<Instant, Long, Long> paused = pause.get();
        final long countWhenPaused = paused._3;
        if (countWhenPaused < 0) { // there is no request counter, which is some weird test-only situation
            pause.set(null);
            LOGGER.warn("No request counter. This should not be happening.");
            return true;
        }
        final long currentCount = getRequestCount();
        if (countWhenPaused == currentCount) {
            /*
             * this is not a guarantee of the exception being gone, in case it was a HTTP 429 Too Many Requests.
             * unfortunately, this will also keep the robot paused in situations where the error was something else,
             * but thankfully the robot never goes much longer than a few seconds without making a metered call.
             */
            LOGGER.debug("Not resuming as there were no metered requests since.");
            return false;
        } else {
            pause.set(null);
            LOGGER.info("Resumed after a forced pause.");
            return true;
        }
    }

    private long getRequestCount() {
        return counter == null ? -1 : counter.count();
    }

    @Override
    public boolean registerException(final Exception ex) {
        if (isAvailable()) {
            pause.set(Tuple.of(DateUtil.now(), 0L, getRequestCount()));
            LOGGER.debug("Fault identified, forcing pause.", ex);
            // will go to console, no stack trace
            LOGGER.warn("Forcing a pause due to a potentially irrecoverable fault.");
            return true;
        } else {
            final Tuple3<Instant, Long, Long> paused = pause.updateAndGet(f -> Tuple.of(f._1, f._2 + 1, f._3));
            LOGGER.debug("Forced pause in effect since {}, {} retries.", paused._1, paused._2, ex);
            return false;
        }
    }
}
