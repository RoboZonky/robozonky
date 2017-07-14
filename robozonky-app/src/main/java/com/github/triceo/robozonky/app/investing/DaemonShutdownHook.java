/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.investing;

import java.util.concurrent.CountDownLatch;

import com.github.triceo.robozonky.app.ShutdownEnabler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DaemonShutdownHook extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonShutdownHook.class);

    private final CountDownLatch blockUntilZero;

    public DaemonShutdownHook(final CountDownLatch blockUntilZero) {
        this.blockUntilZero = blockUntilZero;
    }

    public void run() {
        LOGGER.debug("Shutdown requested.");
        // will release the main thread and thus terminate the daemon
        blockUntilZero.countDown();
        // only allow to shut down after the daemon has been closed by the app
        try {
            ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE.await();
        } catch (final InterruptedException ex) { // don't block shutdown indefinitely
            LOGGER.warn("Timed out waiting for daemon to terminate cleanly.");
        } finally {
            LOGGER.debug("Shutdown allowed.");
        }
    }
}
