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

package com.github.robozonky.app.daemon;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyDaemonResumed;
import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyDaemonSuspended;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.test.DateUtil;

final class Skippable implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(Skippable.class);

    private final PowerTenant tenant;
    private final Class<?> type;
    private final Runnable toRun;
    private final Consumer<Throwable> shutdownCall;

    Skippable(final Runnable toRun, final Class<?> type, final PowerTenant tenant,
            final Consumer<Throwable> shutdownCall) {
        this.toRun = toRun;
        this.type = type;
        this.tenant = tenant;
        this.shutdownCall = shutdownCall;
    }

    Skippable(final Runnable toRun, final PowerTenant tenant, final Consumer<Throwable> shutdownCall) {
        this(toRun, toRun.getClass(), tenant, shutdownCall);
    }

    Skippable(final Runnable toRun, final PowerTenant tenant) {
        this(toRun, tenant, t -> {
            // do nothing
        });
    }

    @Override
    public void run() {
        final Availability availability = tenant.getAvailability();
        final ZonedDateTime nextCheck = availability.nextAvailabilityCheck();
        LOGGER.trace("Next availability check: {}.", nextCheck);
        if (nextCheck.isAfter(DateUtil.zonedNow())) {
            LOGGER.debug("Not running {} on account of the robot being temporarily suspended.", this);
            return;
        }
        LOGGER.trace("Running {}.", this);
        try {
            final ZonedDateTime now = DateUtil.zonedNow();
            toRun.run();
            final Optional<ZonedDateTime> becameAvailable = availability.registerSuccess();
            becameAvailable.ifPresent(unavailableSince -> {
                LOGGER.debug("Unavailability over, lasted since {}.", unavailableSince);
                tenant.fire(roboZonkyDaemonResumed(unavailableSince, now));
            });
            LOGGER.trace("Successfully finished {}.", this);
        } catch (final Throwable t) {
            if (SimpleSkippable.isRecoverable(t)) {
                final Exception ex = (Exception) t; // errors are never recoverable
                final boolean becameUnavailable = availability.registerException(ex);
                if (becameUnavailable) {
                    LOGGER.debug("Unavailability starting.");
                    tenant.fire(roboZonkyDaemonSuspended(ex));
                }
            } else {
                shutdownCall.accept(t);
                throw t;
            }
        } finally {
            LOGGER.trace("Finished {}.", this);
        }
    }

    @Override
    public String toString() {
        return "Skippable{" +
                "type=" + type.getCanonicalName() +
                '}';
    }
}
