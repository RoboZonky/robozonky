/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.async;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.github.robozonky.internal.util.functional.Either;

class ReloadableImplTest {

    @Test
    void manually() {
        final Consumer<String> mock = mock(Consumer.class);
        final ReloadListener<String> listener = mock(ReloadListener.class);
        final Reloadable<String> r = Reloadable.with(() -> UUID.randomUUID()
            .toString())
            .finishWith(mock)
            .addListener(listener)
            .build();
        assertThat(r.hasValue()).isFalse();
        final Either<Throwable, String> result = r.get();
        assertThat(r.hasValue()).isTrue();
        assertThat(result.get()).isInstanceOf(String.class);
        verify(mock).accept(any());
        verify(listener, times(1)).newValue(any());
        verify(listener, never()).valueUnset();
        final String value = result.get();
        assertThat(r.get()
            .get()).isEqualTo(value); // new call, no change
        verify(mock, times(1)).accept(any()); // still called just once
        verify(listener, times(1)).newValue(any());
        verify(listener, never()).valueUnset();
        r.clear();
        verify(listener, times(1)).newValue(any());
        verify(listener, times(1)).valueUnset();
        final Either<Throwable, String> result2 = r.get(); // will reload now
        assertThat(result2.get()).isInstanceOf(String.class);
        verify(mock, times(2)).accept(any()); // called for the second time now
        verify(listener, times(2)).newValue(any());
        verify(listener, times(1)).valueUnset();
        assertThat(result2.get()).isNotEqualTo(value);
    }

    @Test
    void timeBased() {
        final Consumer<String> mock = mock(Consumer.class);
        final Reloadable<String> r = Reloadable.with(() -> UUID.randomUUID()
            .toString())
            .reloadAfter(Duration.ofSeconds(5))
            .finishWith(mock)
            .build();
        final Either<Throwable, String> result = r.get();
        assertThat(result.get()).isInstanceOf(String.class);
        verify(mock).accept(any());
        final String value = result.get();
        assertThat(r.get()
            .get()).isEqualTo(value); // new call, no change
        verify(mock, times(1)).accept(any()); // still called just once
    }

    @Test
    void timeBasedNoConsumer() {
        final Reloadable<String> r = Reloadable.with(() -> UUID.randomUUID()
            .toString())
            .reloadAfter(Duration.ofSeconds(5))
            .build();
        final Either<Throwable, String> result = r.get();
        assertThat(result.get()).isInstanceOf(String.class);
        final String value = result.get();
        assertThat(r.get()
            .get()).isEqualTo(value); // new call, no change
    }

    @Test
    void finisherFails() {
        final Consumer<String> finisher = mock(Consumer.class);
        doThrow(IllegalStateException.class).when(finisher)
            .accept(any());
        final ReloadListener<String> listener = mock(ReloadListener.class);
        final Reloadable<String> r = Reloadable.with(() -> "")
            .finishWith(finisher)
            .addListener(listener)
            .build();
        final Either<Throwable, String> result = r.get();
        assertThatThrownBy(result::get).isInstanceOf(NoSuchElementException.class); // no value as the finisher failed
        verify(listener, never()).newValue(any());
        verify(listener, never()).valueUnset();
    }
}
