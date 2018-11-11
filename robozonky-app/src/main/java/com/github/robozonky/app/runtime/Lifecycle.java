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
import com.github.robozonky.internal.util.LazyInitialized;
import com.github.robozonky.util.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

;

/**
 * This class controls the internals of the application. It provides ways of blocking certain robot operations until
 * network is available and Zonky is up. It will automatically {@link Schedulers#pause()} if it detects it is offline.
 */
public class Lifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(Lifecycle.class);

    private final CountDownLatch circuitBreaker;
    private final MainControl livenessCheck;
    private final LazyInitialized<DaemonShutdownHook> shutdownHook;
    private volatile Throwable terminationCause = null;

    /**
     * For testing purposes only.
     */
    public Lifecycle() {
        this(new ShutdownHook());
    }

    public Lifecycle(final ShutdownHook hooks) {
        this(new CountDownLatch(1), hooks);
    }

    Lifecycle(final CountDownLatch circuitBreaker) {
        this(circuitBreaker, new ShutdownHook());
    }

    private Lifecycle(final CountDownLatch circuitBreaker, final ShutdownHook hooks) {
        this(new MainControl(), circuitBreaker, hooks);
    }

    Lifecycle(final MainControl mc) {
        this(mc, null, new ShutdownHook());
    }

    private Lifecycle(final MainControl mc, final CountDownLatch circuitBreaker, final ShutdownHook hooks) {
        this.circuitBreaker = circuitBreaker;
        this.livenessCheck = mc;
        final ShutdownEnabler shutdownEnabler = new ShutdownEnabler();
        this.shutdownHook = LazyInitialized.create(() -> new DaemonShutdownHook(this, shutdownEnabler));
        hooks.register(LivenessCheck.setup(livenessCheck));
        hooks.register(shutdownEnabler);
    }

    /**
     * Will block until RoboZonky is back online.
     * @return True if now online, false if interrupted.
     */
    static boolean waitUntilOnline(final MainControl livenessCheck) {
        try {
            livenessCheck.waitUntilTriggered();
            return true;
        } catch (final InterruptedException ex) {
            LOGGER.error("Cannot reach Zonky.", ex);
            return false;
        }
    }

    public Optional<String> getZonkyApiVersion() {
        return livenessCheck.getApiVersion();
    }

    /**
     * Will block until RoboZonky is back online.
     * @return True if now online, false if interrupted.
     */
    public boolean waitUntilOnline() {
        return waitUntilOnline(livenessCheck);
    }

    /**
     * Suspend thread until either one of {@link #resumeToFail(Throwable)} or {@link #resumeToShutdown()} is called.
     */
    public void suspend() {
        Runtime.getRuntime().addShutdownHook(shutdownHook.get());
        LOGGER.debug("Pausing main thread.");
        try {
            circuitBreaker.await();
        } catch (final InterruptedException ex) {
            LOGGER.warn("Terminating robot unexpectedly.", ex);
        }
    }

    /**
     * Triggered by the daemon to make {@link #suspend()} unblock.
     */
    public void resumeToShutdown() {
        LOGGER.debug("Asking application to shut down cleanly through {}.", this);
        circuitBreaker.countDown();
    }

    /**
     * Triggered by the deamon to make {@link #suspend()} unblock.
     * @param t Will become the value in {@link #getTerminationCause()}.
     */
    public void resumeToFail(final Throwable t) {
        LOGGER.debug("Asking application to die through {}.", this);
        terminationCause = t;
        circuitBreaker.countDown();
    }

    /**
     * The reason why the daemon failed, if any.
     * @return Present if {@link #resumeToFail(Throwable)} had been called previously.
     */
    public Optional<Throwable> getTerminationCause() {
        return Optional.ofNullable(terminationCause);
    }
}
