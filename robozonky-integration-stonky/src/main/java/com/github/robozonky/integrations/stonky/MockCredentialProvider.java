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

import java.util.Optional;

import com.github.robozonky.api.SessionInfo;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential;

final class MockCredentialProvider implements CredentialProvider {

    private final boolean shouldExist;

    MockCredentialProvider(final boolean shouldExist) {
        this.shouldExist = shouldExist;
    }

    @Override
    public boolean credentialExists(final SessionInfo sessionInfo) {
        return shouldExist;
    }

    @Override
    public Optional<Credential> getCredential(final SessionInfo sessionInfo) {
        return shouldExist ? Optional.of(new MockGoogleCredential.Builder().build()) : Optional.empty();
    }
}
