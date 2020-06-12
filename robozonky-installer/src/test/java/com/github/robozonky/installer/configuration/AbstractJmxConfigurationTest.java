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

package com.github.robozonky.installer.configuration;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.github.robozonky.internal.Defaults;

class AbstractJmxConfigurationTest {

    private static Properties load(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, Defaults.CHARSET)) {
            final Properties properties = new Properties();
            properties.load(reader);
            return properties;
        }
    }

    @Test
    void disabled() {
        PropertyConfiguration configuration = PropertyConfiguration.disabledJmx();
        assertThat(configuration.getProperties())
            .containsExactly(Map.entry("com.sun.management.jmxremote", "false"));
    }

    @Test
    void enabledSecure() throws IOException {
        Path installDir = Files.createTempDirectory("robozonky-install");
        Path props = installDir.resolve("management.properties");
        PropertyConfiguration configuration = PropertyConfiguration.enabledJmx("localhost", 7091, true);
        configuration.accept(null, installDir);
        assertThat(load(props)).containsOnly(
                Map.entry("com.sun.management.jmxremote.authenticate", "true"),
                Map.entry("com.sun.management.jmxremote.ssl", "false"),
                Map.entry("com.sun.management.jmxremote.rmi.port", "7091"),
                Map.entry("com.sun.management.jmxremote.port", "7091"));
        assertThat(configuration.getProperties()).containsOnly(
                Map.entry("com.sun.management.config.file", props.toString()),
                Map.entry("com.sun.management.jmxremote", "true"),
                Map.entry("java.rmi.server.hostname", "localhost"),
                Map.entry("jmx.remote.x.notification.buffer.size", "10"));
    }

    @Test
    void enabledInsecure() throws IOException {
        Path installDir = Files.createTempDirectory("robozonky-install");
        Path props = installDir.resolve("management.properties");
        PropertyConfiguration configuration = PropertyConfiguration.enabledJmx("localhost", 7091, false);
        configuration.accept(null, installDir);
        assertThat(load(props)).containsOnly(
                Map.entry("com.sun.management.jmxremote.authenticate", "false"),
                Map.entry("com.sun.management.jmxremote.ssl", "false"),
                Map.entry("com.sun.management.jmxremote.rmi.port", "7091"),
                Map.entry("com.sun.management.jmxremote.port", "7091"));
        assertThat(configuration.getProperties())
            .containsOnly(
                    Map.entry("com.sun.management.config.file", props.toString()),
                    Map.entry("com.sun.management.jmxremote", "true"),
                    Map.entry("java.rmi.server.hostname", "localhost"),
                    Map.entry("jmx.remote.x.notification.buffer.size", "10"));
    }

}
