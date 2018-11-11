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

package com.github.robozonky.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Backoff<T> implements Supplier<Optional<T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Backoff.class);
    private final Operation<T> operation;
    private final BackoffTimeCalculator<T> backoffTimeCalculator;
    private final Duration cancelAfter;
    private Exception lastException = null;

    static <T> Optional<Duration> calculateBackoffTime(final T value, final Duration original, final Duration initial) {
        if (value == null) {
            if (Objects.equals(original, Duration.ZERO)) {
                return Optional.of(initial);
            } else {
                return Optional.of(Duration.ofNanos(original.toNanos() * 2));
            }
        } else { // no more backing off is needed
            return Optional.empty();
        }
    }

    public Backoff(final Operation<T> operation, final BackoffTimeCalculator<T> backoffTimeCalculator,
                   final Duration cancelAfter) {
        this.operation = operation;
        this.backoffTimeCalculator = backoffTimeCalculator;
        this.cancelAfter = cancelAfter;
    }

    /**
     * Implements exponential backoff over a given operation.
     * @param operation Operation to execute.
     * @param initialBackoffTime The minimal non-zero value of the backoff time to start the back-off with following up
     * on failed execution of the operation.
     * @param cancelAfter When the total time spent within the algorithm exceeds this, it will be terminated.
     * @param <O> Return type of the operation.
     * @return Return value of the operation, or empty if not reached in time.
     * @see <a href="https://en.wikipedia.org/wiki/Exponential_backoff">Exponential backoff on Wikipedia</a>
     */
    public static <O> Backoff<O> exponential(final Operation<O> operation, final Duration initialBackoffTime,
                                             final Duration cancelAfter) {
        final BackoffTimeCalculator<O> exponential =
                (value, originalBackoffTime) -> calculateBackoffTime(value, originalBackoffTime, initialBackoffTime);
        return new Backoff<>(operation, exponential, cancelAfter);
    }

    private static void wait(final Duration duration) {
        LOGGER.debug("Will wait for {} milliseconds.", duration.toMillis());
        try {
            Thread.sleep(duration.toMillis());
        } catch (final InterruptedException ex) {
            LOGGER.debug("Wait interrupted.", ex);
        }
    }

    private synchronized <O> Optional<O> execute(final Supplier<O> operation) {
        LOGGER.trace("Will execute {}.", operation);
        try {
            final Optional<O> result = Optional.ofNullable(operation.get());
            this.lastException = null;
            return result;
        } catch (final Exception ex) {
            LOGGER.debug("Operation failed.", ex);
            this.lastException = ex;
            return Optional.empty();
        }
    }

    public synchronized Optional<Exception> getLastException() {
        return Optional.ofNullable(lastException);
    }

    @Override
    public Optional<T> get() {
        Duration backoffTime = Duration.ZERO;
        final Instant startedOn = Instant.now();
        do {
            wait(backoffTime);
            final T result = execute(operation).orElse(null);
            final Optional<Duration> newBackoffTime = backoffTimeCalculator.apply(result, backoffTime);
            if (newBackoffTime.isPresent()) {
                backoffTime = newBackoffTime.get();
            } else {
                LOGGER.trace("Success.");
                return Optional.ofNullable(result);
            }
        } while (startedOn.plus(cancelAfter).isAfter(Instant.now()));
        return Optional.empty();
    }

    @FunctionalInterface
    public interface Operation<T> extends Supplier<T> {

        /**
         * Execute the operation, if unsuccessful, the backoff will be applied to.
         * @return Value returned by the operation, to be consumed by
         * {@link BackoffTimeCalculator#apply(Object, Duration)}. It is recommended that it only be null in case of the
         * operation failing.
         */
        @Override
        T get();
    }

    @FunctionalInterface
    public interface BackoffTimeCalculator<T> extends BiFunction<T, Duration, Optional<Duration>> {

        /**
         * Calculate new backoff time based on the previous result of executing {@link Operation}.
         * @param t Return value of the immediately preceding execution of the operation.
         * @param duration The last back-off time. Will equal {@link Duration#ZERO} if this was the first run.
         * @return New back off time or empty if the value is acceptable.
         */
        @Override
        Optional<Duration> apply(T t, Duration duration);
    }
}
