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
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;

import javax.ws.rs.ClientErrorException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.RequestCounter;
import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.test.DateUtil;

final class AvailabilityImpl implements Availability {

    static final long MANDATORY_DELAY_IN_SECONDS = 5;
    private static final Logger LOGGER = LogManager.getLogger(AvailabilityImpl.class);
    private final ZonkyApiTokenSupplier zonkyApiTokenSupplier;
    private final AtomicReference<Status> pause = new AtomicReference<>();
    private final Predicate<ZonedDateTime> hasNewerRequest;

    public AvailabilityImpl(final ZonkyApiTokenSupplier zonkyTokenSupplier, final RequestCounter requestCounter) {
        this.zonkyApiTokenSupplier = zonkyTokenSupplier;
        if (requestCounter == null) { // for easier testing
            final LongAdder adder = new LongAdder();
            this.hasNewerRequest = instant -> true;
        } else {
            this.hasNewerRequest = requestCounter::hasMoreRecent;
        }
    }

    AvailabilityImpl(final ZonkyApiTokenSupplier zonkyTokenSupplier) {
        this(zonkyTokenSupplier, null);
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
    public ZonedDateTime nextAvailabilityCheck() {
        if (zonkyApiTokenSupplier.isClosed()) {
            LOGGER.debug("Zonky OAuth2 token already closed, can not perform any more operations.");
            return Instant.ofEpochMilli(Long.MAX_VALUE)
                .atZone(Defaults.ZONKYCZ_ZONE_ID);
        } else if (isAvailable()) { // no waiting for anything
            return DateUtil.zonedNow();
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
    public Optional<ZonedDateTime> registerSuccess() {
        if (isAvailable()) {
            return Optional.empty();
        }
        var paused = pause.get();
        var pausedOn = paused.getExceptionRegisteredOn();
        if (hasNewerRequest.test(pausedOn)) {
            pause.set(null);
            LOGGER.info(() -> "Resumed after a forced pause on " + DateUtil.toString(pausedOn) + ".");
            return Optional.of(paused.getExceptionRegisteredOn());
        } else { // make sure we have actually performed a metered operation, safeguarding against HTTP 429
            LOGGER.info(() -> "Not resumed after a forced pause on " + DateUtil.toString(pausedOn) + ".");
            return Optional.empty();
        }
    }

    @Override
    public boolean registerException(final Exception ex) {
        if (isAvailable()) {
            pause.set(new Status(isQuotaLimitHit(ex)));
            LOGGER.debug("Fault identified, forcing pause.", ex);
            // will go to console, no stack trace
            LOGGER.warn("Forcing a pause due to a remote failure.");
            return true;
        } else {
            var paused = pause.updateAndGet(Status::anotherFailure);
            LOGGER.debug(() -> "Forced pause in effect since " + DateUtil.toString(paused.getExceptionRegisteredOn())
                    + ", " + paused.getFailedRetries() + " failed retries.", ex);
            return false;
        }
    }

    private static final class Status {

        private final ZonedDateTime exceptionRegisteredOn;
        private final int failedRetries;
        private final boolean isQuotaLimited;

        public Status(final ZonedDateTime exceptionRegisteredOn, final int failedRetries,
                final boolean isQuotaLimited) {
            this.exceptionRegisteredOn = exceptionRegisteredOn;
            this.failedRetries = failedRetries;
            this.isQuotaLimited = isQuotaLimited;
        }

        public Status(final boolean isQuotaLimited) {
            this(DateUtil.zonedNow(), 0, isQuotaLimited);
        }

        public Status anotherFailure() {
            return new Status(exceptionRegisteredOn, failedRetries + 1, isQuotaLimited);
        }

        public ZonedDateTime getExceptionRegisteredOn() {
            return exceptionRegisteredOn;
        }

        public int getFailedRetries() {
            return failedRetries;
        }

        public boolean isQuotaLimited() {
            return isQuotaLimited;
        }
    }

}
