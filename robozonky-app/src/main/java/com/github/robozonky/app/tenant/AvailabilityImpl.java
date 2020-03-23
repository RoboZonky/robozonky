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

package com.github.robozonky.app.tenant;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongSupplier;

import javax.ws.rs.ClientErrorException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.remote.RequestCounter;
import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.test.DateUtil;

final class AvailabilityImpl implements Availability {

    static final long MANDATORY_DELAY_IN_SECONDS = 5;
    private static final Logger LOGGER = LogManager.getLogger(AvailabilityImpl.class);
    private final ZonkyApiTokenSupplier zonkyApiTokenSupplier;
    private final AtomicReference<Status> pause = new AtomicReference<>();
    private final LongSupplier currentRequestIdSupplier;

    public AvailabilityImpl(final ZonkyApiTokenSupplier zonkyTokenSupplier, final RequestCounter requestCounter) {
        this.zonkyApiTokenSupplier = zonkyTokenSupplier;
        if (requestCounter == null) { // for easier testing
            final LongAdder adder = new LongAdder();
            this.currentRequestIdSupplier = () -> {
                adder.increment();
                return adder.longValue();
            };
        } else {
            this.currentRequestIdSupplier = requestCounter::current;
        }
    }

    AvailabilityImpl(final ZonkyApiTokenSupplier zonkyTokenSupplier) {
        this(zonkyTokenSupplier, null);
    }

    @Override
    public Instant nextAvailabilityCheck() {
        if (zonkyApiTokenSupplier.isClosed()) {
            LOGGER.debug("Zonky OAuth2 token already closed, can not perform any more operations.");
            return Instant.MAX;
        } else if (isAvailable()) { // no waiting for anything
            return DateUtil.now();
        }
        final Status paused = pause.get();
        // add 5 seconds of initial delay to give time to recover from HTTP 429 or whatever other problem there was
        final boolean unavailableDueToQuota = paused.isQuotaLimited();
        final long initialMandatoryDelayInSeconds = unavailableDueToQuota ? 60 : MANDATORY_DELAY_IN_SECONDS;
        final long secondsFromPauseToNextCheck = initialMandatoryDelayInSeconds
                + (long) Math.pow(2, paused.getFailedRetries());
        return paused.getExceptionRegisteredOn()
            .plus(Duration.ofSeconds(secondsFromPauseToNextCheck));
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
        final Status paused = pause.get();
        if (currentRequestIdSupplier.getAsLong() > paused.getLastRequestId()) {
            pause.set(null);
            LOGGER.info("Resumed after a forced pause.");
            return Optional.of(paused.getExceptionRegisteredOn());
        } else { // make sure we have actually performed a metered operation, safeguarding against HTTP 429
            LOGGER.info("Not resuming after a forced pause, request counter ({}) did not change.",
                    paused.getLastRequestId());
            return Optional.empty();
        }
    }

    static boolean isQuotaLimitHit(Throwable throwable) {
        if (throwable == null) {
            return false;
        } else if (throwable instanceof ClientErrorException) {
            final int code = ((ClientErrorException) throwable).getResponse()
                .getStatus();
            if (code == 429) {
                return true;
            }
        }
        return isQuotaLimitHit(throwable.getCause());
    }

    @Override
    public boolean registerException(final Exception ex) {
        if (isAvailable()) {
            pause.set(new Status(currentRequestIdSupplier.getAsLong(), isQuotaLimitHit(ex)));
            LOGGER.debug("Fault identified, forcing pause.", ex);
            // will go to console, no stack trace
            LOGGER.warn("Forcing a pause due to a remote failure.");
            return true;
        } else {
            final Status paused = pause.updateAndGet(f -> f.anotherFailure(currentRequestIdSupplier.getAsLong()));
            LOGGER.debug("Forced pause in effect since {}, {} failed retries.", paused.getExceptionRegisteredOn(),
                    paused.getFailedRetries(), ex);
            return false;
        }
    }

    private static final class Status {

        private final Instant exceptionRegisteredOn;
        private final int failedRetries;
        private final long lastRequestId;
        private final boolean isQuotaLimited;

        public Status(final Instant exceptionRegisteredOn, final int failedRetries, final long currentRequestId,
                final boolean isQuotaLimited) {
            this.exceptionRegisteredOn = exceptionRegisteredOn;
            this.failedRetries = failedRetries;
            this.lastRequestId = currentRequestId;
            this.isQuotaLimited = isQuotaLimited;
        }

        public Status(final long currentRequestId, final boolean isQuotaLimited) {
            this(DateUtil.now(), 0, currentRequestId, isQuotaLimited);
        }

        public Status anotherFailure(final long currentRequestId) {
            return new Status(exceptionRegisteredOn, failedRetries + 1, currentRequestId, isQuotaLimited);
        }

        public Instant getExceptionRegisteredOn() {
            return exceptionRegisteredOn;
        }

        public int getFailedRetries() {
            return failedRetries;
        }

        public long getLastRequestId() {
            return lastRequestId;
        }

        public boolean isQuotaLimited() {
            return isQuotaLimited;
        }
    }

}
