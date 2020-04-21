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

package com.github.robozonky.api.remote.entities;

import static com.github.robozonky.internal.test.DateUtil.offsetNow;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class LastPublishedItemTest {

    @Test
    void equality() {
        final LastPublishedItem l = new LastPublishedItem(1);
        assertThat(l).isEqualTo(l)
            .isNotEqualTo(null)
            .isNotEqualTo("")
            .isNotEqualTo(new LastPublishedItem(l.getId() + 1));
        final LastPublishedItem equal = new LastPublishedItem(l.getId(), l.getDatePublished());
        assertThat(l).isEqualTo(equal);
        assertThat(equal).isEqualTo(l);
        final LastPublishedItem diff = new LastPublishedItem(l.getId(), offsetNow().plus(Duration.ofSeconds(1)));
        assertThat(diff).isEqualTo(l);
        assertThat(l).isEqualTo(diff);
    }
}
