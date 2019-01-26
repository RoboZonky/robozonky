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

package com.github.robozonky.integrations.stonky;

import com.github.robozonky.api.SessionInfo;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.testing.http.MockHttpTransport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class GoogleCredentialProviderTest {

    private static final SessionInfo SESSION_INFO = new SessionInfo("someone@somewhere.cz");
    private static final byte[] EMPTY = new byte[0];

    private final HttpTransport transport = new MockHttpTransport();

    @Test
    void doesNotExistByDefault() {
        final CredentialProvider c = CredentialProvider.live(transport);
        assertThat(c.credentialExists(SESSION_INFO)).isFalse();
    }

    @Test
    void doesNotExistByDefault2() {
        final CredentialProvider c = CredentialProvider.live(transport, "localhost", 8080);
        assertThat(c.credentialExists(SESSION_INFO)).isFalse();
    }

    @Test
    void invalidApiKey() {
        final CredentialProvider c = new GoogleCredentialProvider(transport, "localhost", 0, () -> EMPTY);
        assertThatThrownBy(() -> c.credentialExists(SESSION_INFO)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> c.getCredential(SESSION_INFO)).isInstanceOf(IllegalStateException.class);
    }
}
