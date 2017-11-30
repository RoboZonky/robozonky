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

package com.github.robozonky.test.schedulers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.robozonky.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The goal of this class is to bring background tasks from {@link Scheduler} to the foreground. This will help will
 * test stability and will allow to write assertions against operations performed with the scheduler.
 */
class TestingScheduledExecutorService implements ScheduledExecutorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestingScheduledExecutorService.class);

    @Override
    public ScheduledFuture<?> schedule(final Runnable runnable, final long l, final TimeUnit timeUnit) {
        return scheduleWithFixedDelay(runnable, l, 0, timeUnit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long l, final TimeUnit timeUnit) {
        submit(callable);
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable runnable, final long l, final long l1,
                                                  final TimeUnit timeUnit) {
        submit(runnable);
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable runnable, final long l, final long l1,
                                                     final TimeUnit timeUnit) {
        submit(runnable);
        return null;
    }

    @Override
    public void shutdown() {
        // no need to do anything
    }

    @Override
    public List<Runnable> shutdownNow() {
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(final long l, final TimeUnit timeUnit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(final Callable<T> callable) {
        try {
            callable.call();
        } catch (final Exception ex) {
            LOGGER.warn("Callable failed.", ex);
        }
        return null;
    }

    @Override
    public <T> Future<T> submit(final Runnable runnable, final T t) {
        execute(runnable);
        return null;
    }

    @Override
    public Future<?> submit(final Runnable runnable) {
        execute(runnable);
        return new Future<Object>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                throw new UnsupportedOperationException();
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
            public Object get() throws InterruptedException, ExecutionException {
                return null;
            }

            @Override
            public Object get(final long timeout,
                              final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
        };
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            final Collection<? extends Callable<T>> collection) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> collection, final long l,
                                         final TimeUnit timeUnit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(
            final Collection<? extends Callable<T>> collection) throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> collection, final long l,
                           final TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(final Runnable runnable) {
        try {
            runnable.run();
        } catch (final Exception ex) {
            LOGGER.warn("Task execution failed.", ex);
        }
    }
}
