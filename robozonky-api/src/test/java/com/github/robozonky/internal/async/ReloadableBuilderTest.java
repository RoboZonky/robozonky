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
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.github.robozonky.internal.util.functional.Either;

class ReloadableBuilderTest {

    @Test
    void asyncNoFinisher() {
        final Reloadable<String> s = Reloadable.with(() -> "")
            .reloadAfter(Duration.ofSeconds(5))
            .async()
            .build();
        assertThat(s).isInstanceOf(AsyncReloadableImpl.class);
    }

    @Test
    void asyncWithFinisher() {
        final Reloadable<String> s = Reloadable.with(() -> "")
            .finishWith(x -> {

            })
            .reloadAfter(Duration.ofSeconds(5))
            .async()
            .build();
        assertThat(s).isInstanceOf(AsyncReloadableImpl.class);
    }

    @Test
    void nonLazy() {
        final String test = UUID.randomUUID()
            .toString();
        final Supplier<String> supplier = () -> test;
        final UnaryOperator<String> secondSupplier = old -> UUID.randomUUID()
            .toString();
        final Consumer<String> finisher = mock(Consumer.class);
        final Either<Throwable, Reloadable<String>> s = Reloadable.with(supplier)
            .reloadWith(secondSupplier)
            .finishWith(finisher)
            .buildEager();
        verify(finisher).accept(eq(test)); // value was fetched
        assertThat(s.get()).isNotNull();
        assertThat(s.get()
            .get()
            .get()).isEqualTo(test);
        verify(finisher, times(1)).accept(eq(test)); // was not called again
    }
}
