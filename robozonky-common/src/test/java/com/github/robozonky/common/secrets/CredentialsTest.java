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

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class CredentialsTest {

    private static final SecretProvider SECRETS = SecretProvider.fallback("");

    @Test
    public void tokenLess() {
        final String first = "zonkoid";
        final Credentials cc = new Credentials(first, CredentialsTest.SECRETS);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cc.getToolId()).isEqualTo(first);
            softly.assertThat(cc.getToken()).isEmpty();
        });
    }

    @Test
    public void withToken() {
        final String first = "zonkoid";
        final String second = "password";
        final Credentials cc = new Credentials(first + ':' + second, CredentialsTest.SECRETS);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cc.getToolId()).isEqualTo(first);
            softly.assertThat(cc.getToken()).contains(second.toCharArray());
        });
    }

    @Test
    public void wrong() {
        final String wrong = "zonkoid:password:extra";
        Assertions.assertThatThrownBy(() -> new Credentials(wrong, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
