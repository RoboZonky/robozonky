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

package com.github.robozonky.internal.secrets;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.internal.remote.entities.ZonkyApiTokenImpl;

class FallbackSecretProviderTest {

    private static final String USR = "username";
    private static final String PWD = "password";

    @Test
    void setUsernameAndPassword() {
        final SecretProvider p = SecretProvider.inMemory(FallbackSecretProviderTest.USR,
                FallbackSecretProviderTest.PWD.toCharArray());
        // make sure original values were set
        assertThat(p.getUsername()).isEqualTo(FallbackSecretProviderTest.USR);
        assertThat(p.getPassword()).isEqualTo(FallbackSecretProviderTest.PWD.toCharArray());
        assertThat(p.isPersistent()).isFalse();
    }

    @Test
    void setToken() {
        final SecretProvider p = SecretProvider.inMemory(FallbackSecretProviderTest.USR,
                FallbackSecretProviderTest.PWD.toCharArray());
        // make sure original values were set
        assertThat(p.getToken()).isEmpty();
        final ZonkyApiToken token = new ZonkyApiTokenImpl(UUID.randomUUID()
            .toString(),
                UUID.randomUUID()
                    .toString(),
                299);
        assertThat(p.setToken(token)).isTrue();
        assertThat(p.getToken()).contains(token);
    }

}
