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

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LazyInitializedTest {

    @Test
    void setAndReset() {
        final Random random = new Random(0);
        final LazyInitialized<Integer> lazy = LazyInitialized.create(random::nextInt);
        final int first = lazy.get();
        final int second = lazy.get();
        assertThat(second).isEqualTo(first);
        lazy.reset();
        final int third = lazy.get();
        assertThat(third).isNotEqualTo(second);
    }
}
