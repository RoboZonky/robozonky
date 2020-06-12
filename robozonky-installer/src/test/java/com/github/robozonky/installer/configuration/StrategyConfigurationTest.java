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
import java.util.UUID;

import org.junit.jupiter.api.Test;

class StrategyConfigurationTest {

    @Test
    void local() throws IOException {
        Path original = Files.createTempFile("robozonky-", ".strategy");
        Path installDir = Files.createTempDirectory("robozonky-install");
        StrategyConfiguration configuration = StrategyConfiguration.local(original.toString());
        configuration.accept(null, installDir);
        Path result = installDir.resolve("robozonky-strategy.cfg");
        assertThat(result).exists();
        assertThat(configuration.getFinalLocation())
            .isEqualTo(result.toString());
    }

    @Test
    void remote() {
        StrategyConfiguration configuration = StrategyConfiguration.remote("http://localhost");
        configuration.accept(null, null);
        assertThat(configuration.getFinalLocation()).isEqualTo("http://localhost");
    }

    @Test
    void remoteWrong() {
        StrategyConfiguration configuration = StrategyConfiguration.remote(UUID.randomUUID()
            .toString());
        assertThatThrownBy(() -> configuration.accept(null, null))
            .isInstanceOf(IllegalStateException.class);
    }

}
