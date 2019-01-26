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

import com.github.robozonky.app.tenant.PowerTenant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyDaemonFailed;

final class Skippable implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(Skippable.class);

    private final PowerTenant tenant;
    private final Consumer<Throwable> shutdownCall;
    private final Runnable toRun;

    public Skippable(final Runnable toRun, final PowerTenant tenant, final Consumer<Throwable> shutdownCall) {
        this.toRun = toRun;
        this.tenant = tenant;
        this.shutdownCall = shutdownCall;
    }

    @Override
    public void run() {
        if (!tenant.isAvailable()) {
            LOGGER.debug("Not running {} on account of Zonky token not being available.", toRun);
            return;
        }
        LOGGER.trace("Running {}.", toRun);
        try {
            toRun.run();
            LOGGER.trace("Finished {}.", toRun);
        } catch (final Exception ex) {
            LOGGER.warn("Caught unexpected exception, continuing operation.", ex);
            tenant.fire(roboZonkyDaemonFailed(ex));
        } catch (final Error t) {
            LOGGER.error("Caught unexpected error, terminating.", t);
            shutdownCall.accept(t);
        }
        LOGGER.trace("Update finished.");
    }

    @Override
    public String toString() {
        return "Skippable{" +
                "toRun=" + toRun +
                '}';
    }
}
