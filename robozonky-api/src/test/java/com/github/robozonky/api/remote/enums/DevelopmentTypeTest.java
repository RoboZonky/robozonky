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

package com.github.robozonky.api.remote.enums;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DevelopmentTypeTest {

    @Test
    void hasMetadata() {
        for (final DevelopmentType d : DevelopmentType.values()) {
            assertThat(d.getIds()).isNotEmpty();
            assertThat(d.getSource()).isNotNull();
        }
    }

    @Test
    void deserialize() throws IOException {
        final String json = "\"EC_SUCCESS_BY_ZONKY\"";
        DevelopmentType item = new ObjectMapper().readValue(json, DevelopmentType.class);
        assertThat(item).isEqualTo(DevelopmentType.SUCCESS);
    }

    @Test
    void deserializeFail() {
        final String json = UUID.randomUUID().toString();
        assertThatThrownBy(() -> new ObjectMapper().readValue(json, DevelopmentType.class))
                .isInstanceOf(JsonParseException.class);
    }

    @Test
    void deserializeWrong() {
        final String json = "\"\"";
        assertThatThrownBy(() -> new ObjectMapper().readValue(json, DevelopmentType.class))
                .isInstanceOf(IllegalStateException.class);
    }
}
