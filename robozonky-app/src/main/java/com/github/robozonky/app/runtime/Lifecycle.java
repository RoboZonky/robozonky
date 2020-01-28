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

package com.github.robozonky.app.runtime;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.internal.util.functional.Memoizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class controls the internals of the application. It provides ways of blocking certain robot operations until
 * network is available and Zonky is up.
 */
public class Lifecycle {

    private static final Logger LOGGER = LogManager.getLogger(Lifecycle.class);
    private static final AtomicReference<Set<Thread>> HOOKS = new AtomicReference<>(initShutdownHooks());
    private final CountDownLatch circuitBreaker;
    private final Supplier<DaemonShutdownHook> shutdownHook;
    private final AtomicBoolean failed = new AtomicBoolean(false);

    /**
     * For testing purposes only.
     */
    public Lifecycle() {
        this(new ShutdownHook());
    }

    public Lifecycle(final ShutdownHook hooks) {
        this(new CountDownLatch(1), hooks);
    }

    Lifecycle(final CountDownLatch circuitBreaker, final ShutdownHook hooks) {
        this.circuitBreaker = circuitBreaker;
        final ShutdownEnabler shutdownEnabler = new ShutdownEnabler();
        this.shutdownHook = Memoizer.memoize(() -> new DaemonShutdownHook(this, shutdownEnabler));
        hooks.register(shutdownEnabler);
    }

    private static Set<Thread> initShutdownHooks() {
        return new HashSet<>(0);
    }

    /**
     * For testing purposes. PITest mutations would start these and not kill them, leading to stuck processes.
     */
    public static Collection<Thread> getShutdownHooks() {
        return HOOKS.getAndSet(initShutdownHooks());
    }

    /**
     * Suspend thread until either {@link #resumeToShutdown()} or {@link #resumeToFail(Throwable)} is called.
     */
    public void suspend() {
        Thread.setDefaultUncaughtExceptionHandler((e, err) -> resumeToFail(err));
        final Thread t = shutdownHook.get();
        Runtime.getRuntime().addShutdownHook(t);
        HOOKS.get().add(t);
        LOGGER.debug("Pausing main thread.");
        try {
            circuitBreaker.await();
        } catch (final InterruptedException ex) {
            resumeToFail(ex);
        }
    }

    /**
     * Triggered by the daemon to make {@link #suspend()} unblock.
     */
    public void resumeToShutdown() {
        LOGGER.debug("Asking application to shut down cleanly through {}.", this);
        circuitBreaker.countDown();
    }

    public boolean isFailed() {
        return failed.get();
    }

    /**
     * Triggered by the deamon to make {@link #suspend()} unblock.
     */
    public void resumeToFail(final Throwable t) {
        final boolean failedAlready = failed.getAndSet(true);
        if (failedAlready) { // sometimes two operations would request this at nearly the same time
            return;
        }
        LOGGER.error("Caught unexpected error, terminating.", t);
        Events.global().fire(EventFactory.roboZonkyCrashed(t));
        LOGGER.debug("Asking application to die through {}.", this);
        circuitBreaker.countDown();
    }
}
