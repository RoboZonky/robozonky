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
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.github.robozonky.internal.util.functional.Either;

class AsyncReloadableImplTest {

    @Test
    void timeBased() {
        final Consumer<String> mock = mock(Consumer.class);
        final Reloadable<String> r = Reloadable.with(() -> UUID.randomUUID()
            .toString())
            .reloadAfter(Duration.ofSeconds(5))
            .finishWith(mock)
            .async()
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
            .async()
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
        final Reloadable<String> r = Reloadable.with(() -> "")
            .finishWith(finisher)
            .async()
            .build();
        final Either<Throwable, String> result = r.get();
        assertThatThrownBy(result::get).isInstanceOf(NoSuchElementException.class); // no value as the finisher failed
    }

    @Test
    void performAsyncTest() { // ugly white-box test as this code is nearly untestable from the outside
        final AsyncReloadableImpl<String> r = (AsyncReloadableImpl<String>) Reloadable.with(() -> "")
            .async()
            .build();
        final CompletableFuture<Void> first = r.refreshIfNotAlreadyRefreshing(null);
        assertThat(first).isNotNull();
        first.completeExceptionally(new IllegalStateException());
        final CompletableFuture<Void> second = r.refreshIfNotAlreadyRefreshing(first);
        assertThat(second).isNotNull()
            .isNotSameAs(first);
        final CompletableFuture<Void> third = mock(CompletableFuture.class);
        when(third.isDone()).thenReturn(false);
        assertThat(r.refreshIfNotAlreadyRefreshing(third)).isSameAs(third);
    }
}
