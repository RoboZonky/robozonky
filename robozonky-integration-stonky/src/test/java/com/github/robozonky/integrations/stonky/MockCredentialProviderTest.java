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

import com.github.robozonky.api.SessionInfo;
import com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class MockCredentialProviderTest {

    private static final SessionInfo SESSION_INFO = new SessionInfo("someone@somewhere.cz");

    @Test
    void exists() {
        final CredentialProvider c = CredentialProvider.mock(true);
        assertSoftly(softly -> {
            softly.assertThat(c.credentialExists(SESSION_INFO)).isTrue();
            softly.assertThat(c.getCredential(SESSION_INFO)).isInstanceOf(MockGoogleCredential.class);
        });
    }

    @Test
    void doesNotExist() {
        final CredentialProvider c = CredentialProvider.mock(false);
        assertSoftly(softly -> {
            softly.assertThat(c.credentialExists(SESSION_INFO)).isFalse();
            softly.assertThatThrownBy(() -> c.getCredential(SESSION_INFO)).isInstanceOf(IllegalStateException.class);
        });
    }
}
