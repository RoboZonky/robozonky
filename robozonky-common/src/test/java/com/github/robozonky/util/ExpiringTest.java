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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ExpiringTest {

    @Test
    void retrievesEmpty() {
        final Runnable r = mock(Runnable.class);
        final TestingExpiring<Instant> e = new TestingExpiring<>(() -> null, Duration.ZERO, r);
        assertThat(e.get()).isEmpty();
        verify(r, never()).run();
    }

    @Test
    void retrievesValue() {
        final Runnable r = mock(Runnable.class);
        final TestingExpiring<UUID> e = new TestingExpiring<>(UUID::randomUUID, Duration.ZERO, r);
        final Optional<UUID> value = e.get();
        assertThat(value).isNotEmpty();
        verify(r).run();
        final Optional<UUID> value2 = e.get();
        assertThat(value2)
                .isNotEmpty()
                .isNotEqualTo(value);
        verify(r, times(2)).run();
    }

    @Test
    void retrievesCachedValue() {
        final Runnable r = mock(Runnable.class);
        final TestingExpiring<Instant> e = new TestingExpiring<>(Instant::now, Duration.ofHours(1), r);
        final Optional<Instant> value = e.get();
        assertThat(value).containsInstanceOf(Instant.class);
        verify(r).run();
        final Optional<Instant> value2 = e.get();
        assertThat(value2)
                .containsInstanceOf(Instant.class)
                .isEqualTo(value);
        verify(r).run();
    }

    private static final class TestingExpiring<T> extends Expiring<T> {

        private final Supplier<T> value;

        public TestingExpiring(final Supplier<T> value, final Duration expiration, final Runnable toRun) {
            super(expiration, toRun);
            this.value = value;
        }

        @Override
        protected Optional<T> retrieve() {
            return Optional.ofNullable(value.get());
        }
    }
}
