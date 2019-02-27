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

package com.github.robozonky.app.events;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Guarantees that events are fired in the order in which they are received. Use {@link #fire(Runnable)} to queue an
 * event to be fired.
 */
final class EventFiringQueue {

    public static final EventFiringQueue INSTANCE = new EventFiringQueue();
    private static final Logger LOGGER = LogManager.getLogger();

    private final AtomicLong counter = new AtomicLong(0);
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    EventFiringQueue() {
        // no external instances
    }

    private static void submittable(final Runnable runnable, final long id, final CountDownLatch latch) {
        LOGGER.trace("Queue starting event {}.", id);
        try {
            runnable.run();
        } finally {
            latch.countDown();
            LOGGER.trace("Queue finished processing event {}.", id);
        }
    }

    private static void waitForFiring(final long id, final CountDownLatch latch) {
        try {
            LOGGER.trace("Await event {}.", id);
            latch.await();
        } catch (final InterruptedException e) {
            LOGGER.debug("Event {} interrupted.", id, e);
            Thread.currentThread().interrupt();
        } finally {
            LOGGER.debug("Event {} processed.", id);
        }
    }

    private synchronized void queue(final Runnable runnable, final long id) {
        try {
            queue.put(runnable);
        } catch (final InterruptedException ex) {
            LOGGER.debug("Interrupted while queuing event {}.", id, ex);
            Thread.currentThread().interrupt();
        }
    }

    BlockingQueue<Runnable> getQueue() {
        return queue;
    }

    /**
     * @param runnable The event to be fired. Must never throw any exception.
     * @return Will be complete when the event has been completely fired or when the operation failed.
     */
    public Runnable fire(final Runnable runnable) {
        final long id = counter.getAndIncrement();
        LOGGER.debug("Queueing event {}.", id);
        final CountDownLatch b = new CountDownLatch(1); // the calling thread will wait for the firing thread
        queue(() -> submittable(runnable, id, b), id);
        return () -> waitForFiring(id, b);
    }
}
