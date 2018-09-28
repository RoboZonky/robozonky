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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackoffTest {

    private static final int DURATION = 1000;

    @Mock
    private Backoff.Operation<String> operation;

    @Test
    void exponentialAlwaysFailing() {
        final Duration maxDuration = ofMillis(DURATION);
        final long now = System.nanoTime();
        // never succeed
        when(operation.get()).thenThrow(new IllegalStateException());
        final Backoff<String> b = assertTimeout(maxDuration.multipliedBy(3),
                                                () -> Backoff.exponential(operation, ofMillis(1), maxDuration));
        final Optional<String> result = b.get();
        final Duration took = Duration.ofNanos(System.nanoTime() - now);
        // make sure result was not successful
        assertThat(result).isEmpty();
        assertThat(b.getLastException()).containsInstanceOf(IllegalStateException.class);
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
        final Optional<String> result = b.get();
        final Duration took = Duration.ofNanos(System.nanoTime() - now);
        // make sure we get the propert result
        assertThat(result).contains(resulting);
        assertThat(b.getLastException()).isEmpty();
        // make sure the operation took less than the max duration
        assertThat(took).isLessThan(maxDuration);
    }

    @Test
    void initialBackOffTimeCalculation() {
        final Duration initial = Duration.ofSeconds(123);
        final Optional<Duration> backoff = Backoff.calculateBackoffTime(null, Duration.ZERO, initial);
        assertThat(backoff).contains(initial);
    }

    @Test
    void continuedBackOffTimeCalculation() {
        final Duration initial = Duration.ofSeconds(123);
        final Optional<Duration> backoff = Backoff.calculateBackoffTime(null, Duration.ofSeconds(1), initial);
        assertThat(backoff).contains(Duration.ofSeconds(2));
    }

    @Test
    void noMoreBackoff() {
        final Duration initial = Duration.ofSeconds(123);
        final Optional<Duration> backoff = Backoff.calculateBackoffTime(BigDecimal.TEN, Duration.ofSeconds(1), initial);
        assertThat(backoff).isEmpty();
    }
}

