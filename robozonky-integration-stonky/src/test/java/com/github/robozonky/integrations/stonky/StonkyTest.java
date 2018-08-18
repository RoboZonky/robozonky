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

import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.google.api.client.http.HttpTransport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StonkyTest extends AbstractRoboZonkyTest {

    private static final SecretProvider SECRET_PROVIDER = SecretProvider.inMemory("someone@somewhere.cz");

    private final HttpTransport transport = new MultiRequestMockHttpTransport();
    private final ApiProvider api = mock(ApiProvider.class);

    @Test
    void noCredentials() {
        final CredentialProvider credential = CredentialProvider.mock(false);
        final Stonky stonky = new Stonky(transport, credential, api);
        stonky.accept(SECRET_PROVIDER); // no exception for non-existent credential = ssuccess
        verify(api).close();
    }

    @Nested
    class WithCredential {

        private final CredentialProvider credential = CredentialProvider.mock(true);

        @Test
        void passes() {
            final Stonky stonky = new Stonky(transport, credential, api);
            stonky.accept(SECRET_PROVIDER);
        }
    }
}
