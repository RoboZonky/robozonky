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

package com.github.robozonky.internal.util.json;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CharArrayAdapterTest {

    private final CharArrayAdapter adapter = new CharArrayAdapter();

    @Test
    void marshalAndUnmarshal() {
        final String s = UUID.randomUUID()
            .toString();
        final char[] array = adapter.adaptFromJson(s);
        final String newS = adapter.adaptToJson(array);
        assertThat(newS).isEqualTo(s);
    }

}
