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
import java.util.function.Supplier;

import io.vavr.control.Either;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackoffTest {

    private static final int DURATION = 1000;

    @Mock
    private Supplier<String> operation;

    @Test
    void exponentialAlwaysFailing() {
        final Duration maxDuration = ofMillis(DURATION);
        final long now = System.nanoTime();
        // never succeed
        when(operation.get()).thenThrow(new IllegalStateException());
        final Backoff<String> b = assertTimeout(maxDuration.multipliedBy(3),
                                                () -> Backoff.exponential(operation, ofMillis(1), maxDuration));
        final Either<Throwable, String> result = b.get();
        final Duration took = Duration.ofNanos(System.nanoTime() - now);
        // make sure result was not successful
        VavrAssertions.assertThat(result).containsLeftInstanceOf(IllegalStateException.class);
        // make sure the operation took at least the expected duration
        assertThat(took).isGreaterThan(maxDuration);
        // make sure the operation was tried the expected number of times, the sum of n^2 for n=[0, ...)
        verify(operation, atLeast(10)).get();
    }

    @Test
    void exponentialWillReturn() {
        final String resulting = "";
        final Duration maxDuration = ofMillis(DURATION);
        final long now = System.nanoTime();
        // succeeding immediately
        final Backoff<String> b = assertTimeout(maxDuration.multipliedBy(3),
                                                () -> Backoff.exponential(() -> resulting, ofMillis(1), maxDuration));
        final Either<Throwable, String> result = b.get();
        final Duration took = Duration.ofNanos(System.nanoTime() - now);
        // make sure we get the propert result
        VavrAssertions.assertThat(result).containsRightSame(resulting);
        // make sure the operation took less than the max duration
        assertThat(took).isLessThan(maxDuration);
    }

    @Test
    void initialBackOffTimeCalculation() {
        final Duration initial = Duration.ofSeconds(123);
        final Duration backoff = Backoff.calculateBackoffTime(Duration.ZERO, initial);
        assertThat(backoff).isEqualTo(initial);
    }

    @Test
    void continuedBackOffTimeCalculation() {
        final Duration initial = Duration.ofSeconds(123);
        final Duration backoff = Backoff.calculateBackoffTime(Duration.ofSeconds(1), initial);
        assertThat(backoff).isEqualTo(Duration.ofSeconds(2));
    }

}

