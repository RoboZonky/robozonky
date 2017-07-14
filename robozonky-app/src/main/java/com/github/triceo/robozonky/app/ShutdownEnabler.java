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

package com.github.triceo.robozonky.app;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * Makes sure that when Ctrl+C is pressed in daemon-mode, the app cleanly shuts down.
 */
public class ShutdownEnabler implements ShutdownHook.Handler {

    public static CountDownLatch DAEMON_ALLOWED_TO_TERMINATE = new CountDownLatch(1);

    @Override
    public Optional<Consumer<ShutdownHook.Result>> get() {
        return Optional.of((returnCode -> {
            /*
             * when the code gets here during shutdown, control is handed over to the daemon, which is already
             * waiting to acquire; application will relinquish control and the JVM will shut down.
             */
            ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE.countDown();
            ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE = new CountDownLatch(1);
        }));
    }
}
