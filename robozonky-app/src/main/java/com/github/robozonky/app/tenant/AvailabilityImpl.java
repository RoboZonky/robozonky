/*
 * Copyright 2021 The RoboZonky Project
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

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.test.DateUtil;

import io.micrometer.core.instrument.Timer;

final class AvailabilityImpl implements Availability {

    static final long MANDATORY_DELAY_IN_SECONDS = 5;
    private static final Logger LOGGER = LogManager.getLogger(AvailabilityImpl.class);
    private final ZonkyApiTokenSupplier zonkyApiTokenSupplier;
    private final AtomicReference<Status> pause = new AtomicReference<>();
    private final Timer meteredRequestTimer;
    private final Timer downtimeTimer;
    private final AtomicLong requestCountAtTimeOfError = new AtomicLong(0);

    public AvailabilityImpl(final ZonkyApiTokenSupplier zonkyTokenSupplier, final Timer requestTimer) {
        this.zonkyApiTokenSupplier = zonkyTokenSupplier;
        this.meteredRequestTimer = requestTimer;
        this.downtimeTimer = Timer.builder("robozonky.downtime") // TODO make tenant-specific one day.
            .register(Defaults.METER_REGISTRY);
    }

    AvailabilityImpl(final ZonkyApiTokenSupplier zonkyTokenSupplier) {
        this(zonkyTokenSupplier, null);
    }

    static boolean isQuotaLimitHit(Throwable throwable) {
        if (throwable == null) {
            return false;
        } else if (throwable instanceof ClientErrorException) {
            var code = ((ClientErrorException) throwable).getResponse()
                .getStatus();
            if (code == 429) {
                return true;
            }
        }
        return isQuotaLimitHit(throwable.getCause());
    }

    static boolean canBeIgnored(Throwable throwable) {
        if (throwable == null) {
            return false;
        } else if (throwable instanceof SocketTimeoutException || throwable instanceof InternalServerErrorException) {
            return true;
        }
        return canBeIgnored(throwable.getCause());
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
        var status = pause.get();
        // add 5 seconds of initial delay to give time to recover from HTTP 429 or whatever other problem there was
        var unavailableDueToQuota = status.isQuotaLimited();
        var initialMandatoryDelayInSeconds = unavailableDueToQuota ? 60 : MANDATORY_DELAY_IN_SECONDS;
        var secondsFromPauseToNextCheck = initialMandatoryDelayInSeconds
                + (long) Math.pow(2, status.getFailedRetries());
        return status.getExceptionRegisteredOn()
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
        var hasNewerRequest = requestCountAtTimeOfError.get() < 0 ||
                meteredRequestTimer.count() > requestCountAtTimeOfError.get();
        if (hasNewerRequest) {
            var downtime = Duration.between(paused.getExceptionRegisteredOn(), DateUtil.zonedNow())
                .abs();
            downtimeTimer.record(downtime);
            requestCountAtTimeOfError.set(-1);
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
        if (canBeIgnored(ex)) {
            // Zonky throws some errors relatively frequently.
            // The user can do absolutely nothing about them.
            // So we just ignore them.
            LOGGER.debug("Ignoring Zonky API exception.", ex);
            return false;
        }
        requestCountAtTimeOfError.set(meteredRequestTimer == null ? -1 : meteredRequestTimer.count());
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
