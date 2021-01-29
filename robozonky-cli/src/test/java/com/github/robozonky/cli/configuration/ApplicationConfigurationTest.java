/*
 * Copyright 2021 The RoboZonky Project
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
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ApplicationConfigurationTest {

    private static final String SECRET = UUID.randomUUID()
        .toString();

    @Test
    void dryRun() throws IOException {
        ApplicationConfiguration configuration = (ApplicationConfiguration) PropertyConfiguration
            .applicationDryRun(Files.createTempFile("robozonky-", ".keystore"), SECRET.toCharArray());
        assertThat(configuration.getApplicationArguments())
            .containsEntry("p", SECRET)
            .containsEntry("d", "");
        assertThat(configuration.getJvmArguments())
            .containsEntry("Xmx128m", "");
    }

    @Test
    void realRun() throws IOException {
        ApplicationConfiguration configuration = (ApplicationConfiguration) PropertyConfiguration
            .applicationReal(Files.createTempFile("robozonky-", ".keystore"), SECRET.toCharArray());
        assertThat(configuration.getApplicationArguments())
            .containsEntry("p", SECRET)
            .doesNotContainKey("d");
        assertThat(configuration.getJvmArguments())
            .containsEntry("Xmx64m", "");
    }

}
