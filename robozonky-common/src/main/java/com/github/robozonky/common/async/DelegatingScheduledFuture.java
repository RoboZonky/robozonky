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

package com.github.robozonky.common.async;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The point of this class is to always provide access to the latest {@link ScheduledFuture} as specific in
 * {@link ThreadPoolExecutorBasedScheduler}. It doesn't do anything other than delegate all calls to the most recent
 * {@link ScheduledFuture}.
 * @param <T>
 */
final class DelegatingScheduledFuture<T> implements ScheduledFuture<T> {

    private final AtomicReference<ScheduledFuture<?>> current = new AtomicReference<>();

    void setCurrent(final ScheduledFuture<?> newFuture) {
        current.set(newFuture);
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        return current.get().getDelay(unit);
    }

    @Override
    public int compareTo(final Delayed o) {
        return current.get().compareTo(o);
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return current.get().cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return current.get().isCancelled();
    }

    @Override
    public boolean isDone() {
        return current.get().isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return (T) current.get().get();
    }

    @Override
    public T get(final long timeout,
                 final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return (T) current.get().get(timeout, unit);
    }
}
