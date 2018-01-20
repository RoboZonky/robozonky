/*
 * Copyright 2018 The RoboZonky Project
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
import java.util.concurrent.CountDownLatch;

import com.github.robozonky.app.ShutdownHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeHandler.class);

    private final ShutdownEnabler shutdownEnabler;
    private final DaemonShutdownHook shutdownHook;
    private final CountDownLatch circuitBreaker;
    private volatile Throwable terminationCause = null;

    public RuntimeHandler() {
        this(new CountDownLatch(1));
    }

    RuntimeHandler(final CountDownLatch circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        this.shutdownEnabler = new ShutdownEnabler();
        this.shutdownHook = new DaemonShutdownHook(this, shutdownEnabler);
    }

    public void suspend() {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        LOGGER.debug("Pausing main thread.");
        try {
            circuitBreaker.await();
        } catch (final InterruptedException ex) {
            LOGGER.warn("Terminating robot unexpectedly.", ex);
        }
    }

    public ShutdownHook.Handler getShutdownEnabler() {
        return shutdownEnabler;
    }

    public void resumeToShutdown() {
        LOGGER.debug("Asking application to shut down cleanly through {}.", this);
        circuitBreaker.countDown();
    }

    public void resumeToFail(final Throwable t) {
        LOGGER.debug("Asking application to die through {}.", this);
        terminationCause = t;
        circuitBreaker.countDown();
    }

    public Optional<Throwable> getTerminationCause() {
        return Optional.ofNullable(terminationCause);
    }
}
