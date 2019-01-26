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

package com.github.robozonky.common.secrets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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
        // set some secrets
        final String key = "key", value = "value";
        assertThat(p.getSecret(key)).isEmpty();
        assertThat(p.setSecret(key, value.toCharArray())).isTrue();
        assertThat(p.getSecret(key)).contains(value.toCharArray());
        assertThat(p.isPersistent()).isFalse();
    }
}
