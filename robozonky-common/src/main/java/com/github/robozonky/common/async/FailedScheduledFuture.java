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

import java.util.Comparator;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class FailedScheduledFuture<T> implements ScheduledFuture<T> {

    private static final Comparator<Delayed> COMPARATOR =
            Comparator.comparing(delayed -> delayed.getDelay(TimeUnit.NANOSECONDS));
    private final Exception exception;

    public FailedScheduledFuture(final Exception ex) {
        this.exception = ex;
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        return 0;
    }

    @Override
    public int compareTo(final Delayed o) {
        return COMPARATOR.compare(this, o);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        throw new ExecutionException(exception);
    }

    @Override
    public T get(final long timeout,
                 final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new ExecutionException(exception);
    }
}
