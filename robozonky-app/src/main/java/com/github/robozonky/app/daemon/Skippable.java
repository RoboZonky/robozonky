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

import com.github.robozonky.app.tenant.PowerTenant;
import jdk.jfr.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyDaemonFailed;

final class Skippable implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(Skippable.class);

    private final PowerTenant tenant;
    private final Class<?> type;
    private final Runnable toRun;

    public Skippable(final Runnable toRun, final Class<?> type, final PowerTenant tenant) {
        this.toRun = toRun;
        this.type = type;
        this.tenant = tenant;
    }

    Skippable(final Runnable toRun, final PowerTenant tenant) {
        this.toRun = toRun;
        this.type = toRun.getClass();
        this.tenant = tenant;
    }

    @Override
    public void run() {
        final Event event = new SkippableJfrEvent();
        event.begin();
        if (!tenant.isAvailable()) {
            LOGGER.debug("Not running {} on account of Zonky token not being available.", this);
            return;
        }
        LOGGER.trace("Running {}.", this);
        try {
            toRun.run();
            LOGGER.trace("Finished {}.", this);
        } catch (final Exception ex) {
            LOGGER.warn("Caught unexpected exception, continuing operation.", ex);
            tenant.fire(roboZonkyDaemonFailed(ex));
        } finally {
            event.commit();
        }
    }

    @Override
    public String toString() {
        return "Skippable{" +
                "type=" + type.getCanonicalName() +
                '}';
    }
}
