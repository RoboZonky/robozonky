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

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.github.robozonky.internal.util.DateUtil;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Backoff<T> implements Supplier<Either<Throwable, T>> {

    private static final Logger LOGGER = LogManager.getLogger(Backoff.class);
    private final Supplier<T> operation;
    private final BackoffTimeCalculator backoffTimeCalculator;
    private final Duration cancelAfter;

    private Backoff(final Supplier<T> operation, final BackoffTimeCalculator backoffTimeCalculator,
                    final Duration cancelAfter) {
        this.operation = operation;
        this.backoffTimeCalculator = backoffTimeCalculator;
        this.cancelAfter = cancelAfter;
    }

    static Duration calculateBackoffTime(final Duration original, final Duration initial) {
        if (Objects.equals(original, Duration.ZERO)) {
            return initial;
        } else {
            return Duration.ofNanos(original.toNanos() * 2);
        }
    }

    /**
     * Implements exponential backoff over a given operation.
     * @param operation Operation to execute. Null is not a permitted return value of the {@link Supplier}.
     * @param initialBackoffTime The minimal non-zero value of the backoff time to start the back-off with following up
     * on failed execution of the operation.
     * @param cancelAfter When the total time spent within the algorithm exceeds this, it will be terminated.
     * @param <O> Return type of the operation.
     * @return Return value of the operation, or empty if not reached in time.
     * @see <a href="https://en.wikipedia.org/wiki/Exponential_backoff">Exponential backoff on Wikipedia</a>
     */
    public static <O> Backoff<O> exponential(final Supplier<O> operation, final Duration initialBackoffTime,
                                             final Duration cancelAfter) {
        final BackoffTimeCalculator exponential = (originalBackoffTime) -> calculateBackoffTime(originalBackoffTime,
                                                                                                initialBackoffTime);
        return new Backoff<>(operation, exponential, cancelAfter);
    }

    private static void wait(final Duration duration) {
        LOGGER.debug("Will wait for {} milliseconds.", duration.toMillis());
        try {
            Thread.sleep(duration.toMillis());
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Wait interrupted.", ex);
        }
    }

    private static <O> Either<Throwable, O> execute(final Supplier<O> operation) {
        LOGGER.trace("Will execute {}.", operation);
        return Try.ofSupplier(operation).map(Either::<Throwable, O>right)
                .recover(t -> {
                    LOGGER.debug("Operation failed.", t);
                    return Either.left(t);
                }).get();
    }

    @Override
    public Either<Throwable, T> get() {
        Duration backoffTime = Duration.ZERO;
        final Instant startedOn = DateUtil.now();
        do {
            wait(backoffTime);
            final Either<Throwable, T> result = execute(operation);
            if (result.isRight()) {
                LOGGER.trace("Success.");
                return Either.right(result.get());
            } else if (startedOn.plus(cancelAfter).isBefore(DateUtil.now())) {
                LOGGER.trace("Expired.");
                return Either.left(result.getLeft());
            }
            // need to try again
            backoffTime = backoffTimeCalculator.apply(backoffTime);
        } while (true);
    }

    @FunctionalInterface
    private interface BackoffTimeCalculator extends UnaryOperator<Duration> {

        /**
         * Calculate new backoff time based on the previous backoff time.
         * @param duration The last back-off time. Will equal {@link Duration#ZERO} if this was the first run.
         * @return New back off time.
         */
        @Override
        Duration apply(Duration duration);
    }
}
