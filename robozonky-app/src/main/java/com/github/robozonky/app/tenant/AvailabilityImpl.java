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
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.ResponseProcessingException;

import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.test.DateUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class AvailabilityImpl implements Availability {

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
        } else if (!isPaused()) { // no waiting for anything
            return DateUtil.now();
        }
        final Tuple2<Instant, Long> paused = pause.get();
        final long retries = paused._2;
        final long secondsFromPauseToNextCheck = (long) Math.pow(2, retries);
        return paused._1.plus(Duration.ofSeconds(secondsFromPauseToNextCheck));
    }

    @Override
    public boolean isPaused() {
        return pause.get() != null;
    }

    @Override
    public void registerAvailability() {
        if (!isPaused()) {
            return;
        }
        pause.set(null);
        LOGGER.info("Resumed after a forced pause.");
    }

    private void register(final Exception ex) {
        if (isPaused()) {
            final Tuple2<Instant, Long> paused = pause.updateAndGet(f -> Tuple.of(f._1, f._2 + 1));
            LOGGER.debug("Forced pause in effect since {}, {} retries.", paused._1, paused._2, ex);
        } else {
            pause.set(Tuple.of(DateUtil.now(), 0L));
            LOGGER.debug("Fault identified, forcing pause.", ex);
            // will go to console, no stack trace
            LOGGER.warn("Forcing a pause due to a potentially irrecoverable fault.");
        }
    }

    @Override
    public void registerApiIssue(final ResponseProcessingException ex) {
        register(ex);
    }

    @Override
    public void registerServerError(final ServerErrorException ex) {
        register(ex);
    }

    @Override
    public void registerClientError(final ClientErrorException ex) {
        register(ex);
    }
}
