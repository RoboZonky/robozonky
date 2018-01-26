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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SchedulersTest {

    @Test
    void create() {
        try (final Scheduler s = Schedulers.INSTANCE.create()) {
            assertThat(s.isPaused()).isFalse();
            Schedulers.INSTANCE.pause();
            assertThat(s.isPaused()).isTrue();
            Schedulers.INSTANCE.resume();
            assertThat(s.isPaused()).isFalse();
        }
    }

    @Test
    void overPause() {
        try (final Scheduler s = Schedulers.INSTANCE.create()) {
            assertThat(s.isPaused()).isFalse();
            Schedulers.INSTANCE.pause();
            assertThat(s.isPaused()).isTrue();
            Schedulers.INSTANCE.pause();
            Schedulers.INSTANCE.resume();
            assertThat(s.isPaused()).isTrue();
            Schedulers.INSTANCE.resume();
            assertThat(s.isPaused()).isFalse();
            Schedulers.INSTANCE.resume();
        }
    }
}
