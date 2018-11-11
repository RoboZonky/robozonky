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

import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

class StonkyIT extends AbstractRoboZonkyTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "ZONKY_USERNAME", matches = ".+")
    void execute() {
        final String username = System.getenv("ZONKY_USERNAME");
        final String password = System.getenv("ZONKY_PASSWORD");
        final SecretProvider secretProvider = SecretProvider.inMemory(username, password.toCharArray());
        final Tenant t = mockTenant();
        doReturn(secretProvider).when(t).getSecrets();
        final Stonky stonky = new Stonky();
        assertThat(stonky.apply(t)).isPresent();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "CREDENTIALS_JSON", matches = ".+")
    void encrypt() throws Exception {
        ApiKey.main(System.getenv("CREDENTIALS_JSON"));
    }
}
