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

package com.github.robozonky.cli;

import java.io.InputStream;

import com.github.robozonky.integrations.stonky.CredentialProvider;
import com.github.robozonky.internal.api.Defaults;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.testing.http.MockHttpTransport;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleCredentialsFeatureTest {

    private InputStream systemIn = System.in;

    @BeforeEach
    void replaceSystemIn() {
        System.setIn(IOUtils.toInputStream("", Defaults.CHARSET));
    }

    @AfterEach
    void restoreSystemIn() {
        System.setIn(systemIn);
    }

    @Test
    void setupAndTest() throws SetupFailedException {
        final HttpTransport transport = new MockHttpTransport();
        final CredentialProvider credentialProvider = CredentialProvider.mock(true);
        final GoogleCredentialsFeature feature =
                new GoogleCredentialsFeature("someone@somewhere.cz", transport, credentialProvider);
        feature.setup();
        // this will fail and is too hard to mock away; doesn't matter much, this is all tested in Sstonky module
        assertThatThrownBy(feature::test).isInstanceOf(TestFailedException.class);
    }

}
