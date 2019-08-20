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

package com.github.robozonky.app.daemon;

import java.util.function.Consumer;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.ResponseProcessingException;

import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.test.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyDaemonFailed;

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

    private static Exception identifyKnownRootCause(final Throwable ex) {
        if (ex instanceof ClientErrorException || ex instanceof ServerErrorException
                || ex instanceof ResponseProcessingException) {
            return (Exception) ex;
        }
        final Throwable cause = ex.getCause();
        if (cause == null) { // no known exception, no deeper cause
            return null;
        } else { // no known exception, recurse into cause
            return identifyKnownRootCause(cause);
        }
    }

    @Override
    public void run() {
        final Availability availability = tenant.getAvailability();
        if (!availability.nextAvailabilityCheck().isBefore(DateUtil.now())) {
            LOGGER.debug("Not running {} on account of the robot being temporarily suspended.", this);
            return;
        }
        LOGGER.trace("Running {}.", this);
        try {
            toRun.run();
            availability.registerAvailability();
            LOGGER.trace("Successfully finished {}.", this);
        } catch (final Exception ex) {
            final Exception cause = identifyKnownRootCause(ex);
            if (cause instanceof ServerErrorException) {
                availability.registerServerError((ServerErrorException) cause);
            } else if (cause instanceof ClientErrorException) {
                availability.registerClientError((ClientErrorException) cause);
            } else if (cause instanceof ResponseProcessingException) {
                availability.registerApiIssue((ResponseProcessingException) cause);
            } else {
                LOGGER.warn("Caught unexpected exception, continuing operation.", ex);
                tenant.fire(roboZonkyDaemonFailed(ex));
            }
        } catch (final Error er) {
            shutdownCall.accept(er);
            throw er; // rethrow the error
        }
    }

    @Override
    public String toString() {
        return "Skippable{" +
                "type=" + type.getCanonicalName() +
                '}';
    }
}
