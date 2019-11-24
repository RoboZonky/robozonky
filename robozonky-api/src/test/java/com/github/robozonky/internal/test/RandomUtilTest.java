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

package com.github.robozonky.internal.test;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class RandomUtilTest {

    private final Random random = mock(Random.class);

    @AfterEach
    void resetRandom() {
        RandomUtil.resetRandom();
    }

    @Test
    void replacesWithSynthetic() {
        final int first = 123456;
        when(random.nextInt()).thenReturn(first);
        when(random.nextInt(anyInt())).thenAnswer(i -> i.getArgument(0));
        RandomUtil.setRandom(random);
        assertSoftly(softly -> {
            softly.assertThat(RandomUtil.getNextInt()).isEqualTo(first);
            final int second = 234567;
            softly.assertThat(RandomUtil.getNextInt(second)).isEqualTo(second);
        });
        RandomUtil.resetRandom();
        assertThat(RandomUtil.getNextInt()).isNotEqualTo(first);
    }
}
