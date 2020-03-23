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

package com.github.robozonky.app;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyEnding;
import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyInitialized;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.internal.Defaults;

/**
 * Will send {@link RoboZonkyInitializedEvent} immediately and {@link RoboZonkyEndingEvent} when it's time to shut down
 * the app.
 */
class RoboZonkyStartupNotifier implements ShutdownHook.Handler {

    private static final Logger LOGGER = LogManager.getLogger(RoboZonkyStartupNotifier.class);

    private final String sessionName;

    public RoboZonkyStartupNotifier(final SessionInfo session) {
        this.sessionName = session.getName();
    }

    @Override
    public Optional<Consumer<ReturnCode>> get() {
        LOGGER.info("===== {} v{} at your service! =====", sessionName, Defaults.ROBOZONKY_VERSION);
        Events.global()
            .fire(roboZonkyInitialized());
        return Optional.of(result -> {
            final CompletableFuture<?> waitUntilFired = Events.global()
                .fire(roboZonkyEnding());
            try {
                LOGGER.debug("Waiting for events to be processed.");
                waitUntilFired.join();
            } catch (final Exception ex) {
                LOGGER.debug("Exception while waiting for the final event being processed.", ex);
            } finally {
                LOGGER.info("===== {} out. =====", sessionName);
            }
        });
    }
}
