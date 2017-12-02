/*
 * Copyright 2017 The RoboZonky Project
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

import java.util.Optional;
import java.util.function.Consumer;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will send {@link RoboZonkyInitializedEvent} immediately and {@link RoboZonkyEndingEvent} when it's time to shut down
 * the app.
 */
class RoboZonkyStartupNotifier implements ShutdownHook.Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoboZonkyStartupNotifier.class);

    @Override
    public Optional<Consumer<ShutdownHook.Result>> get() {
        RoboZonkyStartupNotifier.LOGGER.info("===== RoboZonky v{} at your service! =====", Defaults.ROBOZONKY_VERSION);
        Events.fire(new RoboZonkyInitializedEvent());
        return Optional.of((result) -> {
            if (result.getReturnCode() == ReturnCode.OK) {
                Events.fire(new RoboZonkyEndingEvent());
            } else {
                Events.fire(new RoboZonkyCrashedEvent(result.getReturnCode(), result.getCause()));
            }
            RoboZonkyStartupNotifier.LOGGER.info("===== RoboZonky out. =====");
        });
    }
}
