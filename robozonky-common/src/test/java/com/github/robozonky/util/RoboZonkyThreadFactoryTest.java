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

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class RoboZonkyThreadFactoryTest {

    private static void newThread(final int priority, final boolean isDaemon) {
        final ThreadGroup tg = new ThreadGroup("testing");
        tg.setMaxPriority(priority);
        tg.setDaemon(isDaemon);
        final RoboZonkyThreadFactory f = new RoboZonkyThreadFactory(tg);
        final Thread t = f.newThread(() -> {
        });
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(t.getPriority()).isEqualTo(priority);
            softly.assertThat(t.isDaemon()).isEqualTo(isDaemon);
        });
    }

    @Test
    public void importantDaemon() {
        newThread(Thread.MAX_PRIORITY, true);
    }

    @Test
    public void unimportantRegular() {
        newThread(Thread.MIN_PRIORITY, false);
    }
}
