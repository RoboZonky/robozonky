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

package com.github.robozonky.integrations.stonky;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.github.robozonky.common.secrets.SecretProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

class StonkyIntegrationTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "ZONKY_USERNAME", matches = ".+")
    void execute() throws GeneralSecurityException, IOException {
        final String username = System.getenv("ZONKY_USERNAME");
        final String password = System.getenv("ZONKY_PASSWORD");
        final SecretProvider secretProvider = SecretProvider.inMemory(username, password.toCharArray());
        final Stonky stonky = new Stonky();
        assertThat(stonky.apply(secretProvider)).isPresent();
    }
}
