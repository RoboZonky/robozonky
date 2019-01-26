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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import com.github.robozonky.common.async.Reloadable;
import com.github.robozonky.common.async.Scheduler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Guarantees that events are fired in the order in which they are received. Use {@link #fire(Runnable)} to queue an
 * event to be fired.
 */
final class EventFiringQueue {

    public static final EventFiringQueue INSTANCE = new EventFiringQueue();

    private static final Logger LOGGER = LogManager.getLogger(EventFiringQueue.class);
    private final AtomicLong counter = new AtomicLong(0);
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final Reloadable<Thread> firingThread;

    private EventFiringQueue() {
        this(EventFiringRunnable::new);
    }

    /**
     * Only use for testing purposes.
     * @param threadSupplier
     */
    EventFiringQueue(final Function<BlockingQueue<Runnable>, EventFiringRunnable> threadSupplier) {
        firingThread = Reloadable.with(() -> {
            final Runnable r = threadSupplier.apply(queue);
            LOGGER.debug("Creating new thread with {}.", r);
            final Thread t = Scheduler.THREAD_FACTORY.newThread(r);
            t.start();
            LOGGER.debug("Started event firing thread {}.", t.getName());
            return t;
        }).build();
    }

    private static void await(final CyclicBarrier barrier, final long id) {
        try {
            LOGGER.trace("Await event {}.", id);
            barrier.await();
        } catch (final InterruptedException | BrokenBarrierException e) {
            LOGGER.debug("Event {} interrupted.", id, e);
            Thread.currentThread().interrupt();
        } finally {
            LOGGER.trace("Await over for event {}.", id);
        }
    }

    private void ensureConsumerIsAlive() {
        ensureConsumerIsAlive(false);
    }

    private void ensureConsumerIsAlive(final boolean isRestarted) {
        final boolean isReady = firingThread.get().fold(t -> {
            LOGGER.debug("Failed retrieving event firing thread.", t);
            return false;
        }, Thread::isAlive);
        if (isReady) { // the thread is available
            return;
        } else if (isRestarted) { // the thread is not available even though it really should have been by now
            throw new IllegalStateException("Event firing thread could not be started.");
        } else {
            LOGGER.debug("Consumer thread not alive, restarting.");
            firingThread.clear();
            ensureConsumerIsAlive(true); // "true" here prevents endless recursion
        }
    }

    private synchronized void queue(final Runnable runnable, final long id) {
        ensureConsumerIsAlive(); // lazy creation of the thread that will be emptying the queue
        try {
            queue.put(runnable);
        } catch (final InterruptedException ex) {
            LOGGER.debug("Interrupted while queuing event {}.", id, ex);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @param runnable The event to be fired. Must never throw any exception.
     * @return Will be complete when the event has been completely fired or when the operation failed.
     */
    public CompletableFuture<Void> fire(final Runnable runnable) {
        final long id = counter.getAndIncrement();
        LOGGER.debug("Queueing event {}.", id);
        final CyclicBarrier b = new CyclicBarrier(2); // the request thread and the processing thread
        final Runnable toSubmit = () -> {
            LOGGER.trace("Queue starting event {}.", id);
            try {
                runnable.run();
            } finally {
                await(b, id);
                LOGGER.trace("Queue finished processing event {}.", id);
            }
        };
        queue(toSubmit, id);
        return CompletableFuture.runAsync(() -> {
            await(b, id);
            LOGGER.debug("Event {} processed.", id);
        }, Scheduler.inBackground().getExecutor());
    }
}
