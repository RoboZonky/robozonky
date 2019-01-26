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

package com.github.robozonky.util;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamUtilTest {

    @Test
    void notParallel() {
        final Collection<String> c = Collections.emptyList();
        final Stream<String> s = StreamUtil.toStream(c);
        assertThat(s.isParallel()).isFalse();
    }

    @Mock
    private Consumer<String> consumer;

    @Test
    void toFunction() {
        final String tested = UUID.randomUUID().toString();
        final Function<String, String> f = StreamUtil.toFunction(consumer);
        assertThat(f.apply(tested)).isEqualTo(tested);
        verify(consumer).accept(eq(tested));
    }
}
