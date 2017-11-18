/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.internal.api;

import java.util.Optional;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class RetrieverTest {

    @Test
    public void throwing() {
        final Supplier<Optional<String>> s = Mockito.mock(Supplier.class);
        Mockito.doThrow(IllegalStateException.class).when(s).get();
        Assertions.assertThat(Retriever.retrieve(s)).isEmpty();
    }

    @Test
    public void proper() {
        final Supplier<Optional<String>> s = () -> Optional.of("");
        Assertions.assertThat(Retriever.retrieve(s)).isPresent().contains("");
    }

    @Test
    public void internal() {
        final Supplier<Optional<String>> s = () -> Optional.of("");
        final Retriever<String> r = new Retriever<>(s);
        Assertions.assertThat(r.isReleasable()).isFalse();
        final Optional<String> result = Retriever.retrieve(r);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).contains("");
            softly.assertThat(r.isReleasable()).isTrue();
        });
    }
}
