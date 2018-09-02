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
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;

public interface CredentialProvider {

    /**
     *
     * @param transport
     * @param callbackHost
     * @param callbackPort 0 = auto-detect an open port
     * @return
     */
    static CredentialProvider live(final HttpTransport transport, final String callbackHost, final int callbackPort) {
        return new GoogleCredentialProvider(transport, callbackHost, callbackPort);
    }

    static CredentialProvider live(final HttpTransport transport) {
        return new GoogleCredentialProvider(transport, "localhost", 0);
    }

    static CredentialProvider mock(final boolean shouldExist) {
        return new MockCredentialProvider(shouldExist);
    }

    boolean credentialExists(final SessionInfo sessionInfo);

    Credential getCredential(final SessionInfo sessionInfo);
}
