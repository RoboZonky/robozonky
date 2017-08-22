/*
 * Copyright 2017 The RoboZonky Project
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

import java.io.StringReader;
import java.util.UUID;

import com.github.robozonky.common.AbstractStateLeveragingTest;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class FallbackSecretProviderTest extends AbstractStateLeveragingTest {

    private static final String USR = "username";
    private static final String PWD = "password";

    @Test
    public void setUsernameAndPassword() {
        final SecretProvider p = SecretProvider.fallback(FallbackSecretProviderTest.USR,
                                                         FallbackSecretProviderTest.PWD.toCharArray());
        // make sure original values were set
        Assertions.assertThat(p.getUsername()).isEqualTo(FallbackSecretProviderTest.USR);
        Assertions.assertThat(p.getPassword()).isEqualTo(FallbackSecretProviderTest.PWD.toCharArray());
        // set some secrets
        final String key = "key", value = "value";
        Assertions.assertThat(p.getSecret(key)).isEmpty();
        Assertions.assertThat(p.setSecret(key, value.toCharArray())).isTrue();
        Assertions.assertThat(p.getSecret(key)).contains(value.toCharArray());
        Assertions.assertThat(p.isPersistent()).isFalse();
    }

    @Test
    public void tokenManipulation() {
        final SecretProvider p = SecretProvider.fallback(FallbackSecretProviderTest.USR,
                                                         FallbackSecretProviderTest.PWD.toCharArray());
        final String token = UUID.randomUUID().toString();
        // create token
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(p.setToken(new StringReader(token))).isTrue();
        softly.assertThat(p.getToken()).isPresent();
        softly.assertAll();
        // delete created token
        softly = new SoftAssertions();
        softly.assertThat(p.deleteToken()).isTrue();
        softly.assertAll();
    }
}
