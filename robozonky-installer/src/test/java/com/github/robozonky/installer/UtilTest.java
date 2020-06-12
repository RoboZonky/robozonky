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

package com.github.robozonky.installer;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class UtilTest {

    @Test
    void toInt() {
        assertSoftly(softly -> {
            softly.assertThat(Util.toInt("-1"))
                .isEqualTo("-1");
            softly.assertThat(Util.toInt(null))
                .isEqualTo("-1");
        });
    }

    @Test
    void toBoolean() {
        assertSoftly(softly -> {
            softly.assertThat(Util.toBoolean("true"))
                .isEqualTo("true");
            softly.assertThat(Util.toBoolean("false"))
                .isEqualTo("false");
        });
    }

    @Test
    void writeProperties() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("a", "b");
        File file = File.createTempFile("robozonky", "properties");
        Util.writeOutProperties(properties, file);
        try (var reader = Files.newBufferedReader(file.toPath())) {
            var result = new Properties();
            result.load(reader);
            assertThat(result).containsOnly(Map.entry("a", "b"));
        }
    }

    @Test
    void writePropertiesFail() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("a", "b");
        File file = File.createTempFile("robozonky", "properties");
        file.setWritable(false);
        try {
            assertThatThrownBy(() -> Util.writeOutProperties(properties, file))
                .isInstanceOf(AccessDeniedException.class);
        } finally {
            file.setWritable(true);
        }
    }
}
