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

package com.github.robozonky.internal.util;

import java.util.Map;
import java.util.SortedMap;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MapsTest {

    @Test
    void entry() {
        final Map.Entry<String, String> e = Maps.entry("a", "b");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(e.getKey()).isEqualTo("a");
            softly.assertThat(e.getValue()).isEqualTo("b");
        });
    }

    @Test
    void allowsNullValues() {
        final Map.Entry<String, String> e1 = Maps.entry("a", null);
        final Map<String, String> map = Maps.ofEntries(e1);
        assertThat(map).containsExactly(e1);
    }

    @Test
    void ofEntries() {
        final Map.Entry<String, String> e1 = Maps.entry("b", "a");
        final Map.Entry<String, String> e2 = Maps.entry("a", "b");
        final SortedMap<String, String> map = Maps.ofEntriesSorted(e1, e2);
        assertThat(map).containsExactly(e2, e1);
    }
}
