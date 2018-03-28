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

package com.github.robozonky.api.remote.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DateDescriptorTest {

    @Test
    void longForm() {
        final String format = "yyyy-MM-dd'T'HH:mm:ssZ";
        final String date = "2018-03-20T08:00:17.282Z";
        assertThat(DateDescriptor.toOffsetDateTime(format, date)).isNotNull();
    }

    @Test
    void shortForm() {
        final String format = "yyyy-MM";
        final String date = "2018-03";
        assertThat(DateDescriptor.toOffsetDateTime(format, date)).isNotNull();
    }

    @Test
    void wrongForm() {
        final String format = "YYYY-MM";
        final String date = "2018-03";
        assertThatThrownBy(() -> DateDescriptor.toOffsetDateTime(format, date))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(format);
    }
}
