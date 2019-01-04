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

package com.github.robozonky.internal.util;

import java.util.Random;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class RandomUtilTest extends AbstractMinimalRoboZonkyTest {

    private final Random random = mock(Random.class);

    @BeforeEach
    void replaceClock() {
        setRandom(random);
    }

    @Test
    void replacesWithSynthetic() {
        final int first = 123456;
        when(random.nextInt()).thenReturn(first);
        when(random.nextInt(anyInt())).thenAnswer(i -> i.getArgument(0));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(RandomUtil.getNextInt()).isEqualTo(first);
            final int second = 234567;
            softly.assertThat(RandomUtil.getNextInt(second)).isEqualTo(second);
        });
    }
}
