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

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.vavr.control.Either;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ReloadableBuilderTest {

    @Test
    void nonLazy() {
        final String test = UUID.randomUUID().toString();
        final Supplier<String> supplier = () -> test;
        final Consumer<String> finisher = mock(Consumer.class);
        final Either<Throwable, Reloadable<String>> s = Reloadable.with(supplier)
                .finishWith(finisher)
                .buildEager();
        verify(finisher).accept(eq(test)); // value was fetched
        VavrAssertions.assertThat(s)
                .isRight();
        VavrAssertions.assertThat(s.get().get()).containsOnRight(test);
        verify(finisher, times(1)).accept(eq(test)); // was not called again
    }
}
