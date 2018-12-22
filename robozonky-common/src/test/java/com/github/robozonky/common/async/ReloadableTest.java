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

package com.github.robozonky.common.async;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

import io.vavr.control.Either;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.vavr.api.VavrAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ReloadableTest {

    @Test
    void manually() {
        final Consumer<String> mock = mock(Consumer.class);
        final Reloadable<String> r = Reloadable.of(() -> UUID.randomUUID().toString(), mock);
        final Either<Throwable, String> result = r.get();
        assertThat(result).containsRightInstanceOf(String.class);
        verify(mock).accept(any());
        final String value = result.get();
        assertThat(r.get()).containsOnRight(value); // new call, no change
        verify(mock, times(1)).accept(any()); // still called just once
        r.clear();
        final Either<Throwable, String> result2 = r.get();  // will reload now
        assertThat(result2).containsRightInstanceOf(String.class);
        verify(mock, times(2)).accept(any()); // called for the second time now
        Assertions.assertThat(result2.get()).isNotEqualTo(value);
    }

    @Test
    void timeBased() {
        final Consumer<String> mock = mock(Consumer.class);
        final Reloadable<String> r = Reloadable.of(() -> UUID.randomUUID().toString(), Duration.ofSeconds(5), mock);
        final Either<Throwable, String> result = r.get();
        assertThat(result).containsRightInstanceOf(String.class);
        verify(mock).accept(any());
        final String value = result.get();
        assertThat(r.get()).containsOnRight(value); // new call, no change
        verify(mock, times(1)).accept(any()); // still called just once
    }

    @Test
    void timeBasedNoConsumer() {
        final Reloadable<String> r = Reloadable.of(() -> UUID.randomUUID().toString(), Duration.ofSeconds(5));
        final Either<Throwable, String> result = r.get();
        assertThat(result).containsRightInstanceOf(String.class);
        final String value = result.get();
        assertThat(r.get()).containsOnRight(value); // new call, no change
    }

}