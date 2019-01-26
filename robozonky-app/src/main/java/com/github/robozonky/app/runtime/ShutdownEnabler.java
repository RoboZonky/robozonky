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

package com.github.robozonky.app.runtime;

import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import com.github.robozonky.app.ShutdownHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Makes sure that when Ctrl+C is pressed in daemon-mode, the app cleanly shuts down.
 */
class ShutdownEnabler implements ShutdownHook.Handler {

    private static final Logger LOGGER = LogManager.getLogger(ShutdownEnabler.class);
    private final Semaphore daemonAllowedToTerminate = new Semaphore(1);

    public ShutdownEnabler() {
        daemonAllowedToTerminate.acquireUninterruptibly();
    }

    public void waitUntilTriggered() {
        LOGGER.debug("Waiting for shutdown on {}.", daemonAllowedToTerminate);
        daemonAllowedToTerminate.acquireUninterruptibly();
    }

    @Override
    public Optional<Consumer<ShutdownHook.Result>> get() {
        return Optional.of((returnCode -> {
            /*
             * when the code gets here during shutdown, control is handed over to the daemon, which is already
             * waiting to acquire; application will relinquish control and the JVM will shut down.
             */
            ShutdownEnabler.LOGGER.debug("Running with {}.", daemonAllowedToTerminate);
            daemonAllowedToTerminate.release();
        }));
    }
}
