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
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeout;

public class BackoffTest {

    private static final int DURATION = 1000;

    @Test
    public void exponentialAlwaysFailing() {
        final Duration maxDuration = ofMillis(DURATION);
        final long now = System.nanoTime();
        // never succeed
        final Backoff.Operation<String> o = Mockito.mock(Backoff.Operation.class);
        final Backoff<String> b = assertTimeout(maxDuration.multipliedBy(3),
                                                () -> Backoff.exponential(o, ofMillis(1), maxDuration));
        final Optional<String> result = b.get();
        final Duration took = Duration.ofNanos(System.nanoTime() - now);
        // make sure result was not successful
        Assertions.assertThat(result).isEmpty();
        // make sure the operation took at least the expected duration
        Assertions.assertThat(took).isGreaterThan(maxDuration);
        // make sure the operation was tried the expected number of times, the sum of n^2 for n=[0, ...)
        Mockito.verify(o, Mockito.times(11)).get();
    }

    @Test
    public void exponentialWillReturn() {
        final String resulting = "";
        final Duration maxDuration = ofMillis(DURATION);
        final long now = System.nanoTime();
        // succeeding immediately
        final Backoff<String> b = assertTimeout(maxDuration.multipliedBy(3),
                                                () -> Backoff.exponential(() -> resulting, ofMillis(1), maxDuration));
        final Optional<String> result = b.get();
        final Duration took = Duration.ofNanos(System.nanoTime() - now);
        // make sure we get the propert result
        Assertions.assertThat(result).contains(resulting);
        // make sure the operation took less than the max duration
        Assertions.assertThat(took).isLessThan(maxDuration);
    }
}
