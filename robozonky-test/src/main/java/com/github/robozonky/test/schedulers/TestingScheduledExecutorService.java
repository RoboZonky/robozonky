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

package com.github.robozonky.test.schedulers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.github.robozonky.util.PausableScheduledExecutorService;
import com.github.robozonky.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The goal of this class is to bring background tasks from {@link Scheduler} to the foreground. This will help will
 * test stability and will allow to write assertions against operations performed with the scheduler.
 */
class TestingScheduledExecutorService implements PausableScheduledExecutorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestingScheduledExecutorService.class);

    private boolean wasShutdown = false;

    private static ScheduledFuture<?> getFuture() {
        return new ScheduledFuture<Object>() {
            @Override
            public int compareTo(final Delayed o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getDelay(final TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return true;
            }

            @Override
            public boolean isCancelled() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public Object get() {
                return null;
            }

            @Override
            public Object get(final long timeout, final TimeUnit unit) {
                return null;
            }
        };
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable runnable, final long l, final TimeUnit timeUnit) {
        return scheduleWithFixedDelay(runnable, l, 0, timeUnit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long l, final TimeUnit timeUnit) {
        return (ScheduledFuture<V>) submit(callable);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable runnable, final long l, final long l1,
                                                  final TimeUnit timeUnit) {
        return submit(runnable);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable runnable, final long l, final long l1,
                                                     final TimeUnit timeUnit) {
        return submit(runnable);
    }

    @Override
    public void shutdown() {
        this.wasShutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return wasShutdown;
    }

    @Override
    public boolean isTerminated() {
        return wasShutdown;
    }

    @Override
    public boolean awaitTermination(final long l, final TimeUnit timeUnit) {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Future<T> submit(final Callable<T> callable) {
        try {
            callable.call();
            return (Future<T>) getFuture();
        } catch (final Exception ex) {
            LOGGER.warn("Callable failed.", ex);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ScheduledFuture<T> submit(final Runnable runnable, final T t) {
        return (ScheduledFuture<T>) submit(runnable);
    }

    @Override
    public ScheduledFuture<?> submit(final Runnable runnable) {
        execute(runnable);
        return getFuture();
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> collection, final long l,
                                         final TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> collection, final long l, final TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(final Runnable runnable) {
        LOGGER.debug("Started executing task {}.", runnable);
        try {
            runnable.run();
        } catch (final Exception ex) {
            LOGGER.warn("Task execution failed.", ex);
        } finally {
            LOGGER.debug("Finished executing task.");
        }
    }

    @Override
    public void pause() {
        // FIXME implement
    }

    @Override
    public void resume() {
        // FIXME implement
    }
}
