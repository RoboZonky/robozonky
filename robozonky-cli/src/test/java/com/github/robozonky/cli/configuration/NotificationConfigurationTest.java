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

package com.github.robozonky.cli.configuration;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.internal.Defaults;

class NotificationConfigurationTest {

    private static Properties load(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, Defaults.CHARSET)) {
            final Properties properties = new Properties();
            properties.load(reader);
            return properties;
        }
    }

    @Test
    void disabled() {
        NotificationConfiguration notificationConfiguration = NotificationConfiguration.disabled();
        assertThat(notificationConfiguration.getFinalLocation()).isEmpty();
    }

    @Test
    void created() throws IOException {
        Properties properties = new Properties();
        String key = UUID.randomUUID()
            .toString();
        String value = UUID.randomUUID()
            .toString();
        properties.setProperty(key, value);
        Path installDir = Files.createTempDirectory("robozonky-install");
        NotificationConfiguration configuration = NotificationConfiguration.create(properties);
        configuration.accept(null, installDir);
        Path result = installDir.resolve("robozonky-notifications.cfg");
        assertThat(load(result))
            .containsOnly(Map.entry(key, value));
        assertThat(configuration.getFinalLocation())
            .contains("file://" + result);
    }

    @Test
    void reused() throws IOException {
        Path original = Files.createTempFile("robozonky-notifications", ".cfg");
        Path installDir = Files.createTempDirectory("robozonky-install");
        NotificationConfiguration configuration = NotificationConfiguration.reuse(original.toString());
        configuration.accept(null, installDir);
        Path result = installDir.resolve("robozonky-notifications.cfg");
        assertThat(result).exists();
        assertThat(configuration.getFinalLocation())
            .contains("file://" + result);
    }

    @Test
    void remote() {
        NotificationConfiguration configuration = NotificationConfiguration.remote("http://localhost");
        configuration.accept(null, null);
        assertThat(configuration.getFinalLocation()).contains("http://localhost");
    }

    @Test
    void remoteWrong() {
        NotificationConfiguration configuration = NotificationConfiguration.remote(UUID.randomUUID()
            .toString());
        assertThatThrownBy(() -> configuration.accept(null, null))
            .isInstanceOf(IllegalStateException.class);
    }

}
