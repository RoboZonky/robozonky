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

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.UUID;

import com.github.robozonky.common.secrets.KeyStoreHandler;
import com.github.robozonky.common.secrets.SecretProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZonkoidPasswordFeatureTest {

    private static final String KEYSTORE_PASSWORD = "pwd";

    private static File newTempFile() throws IOException {
        final File f = File.createTempFile("robozonky-", ".keystore");
        f.delete();
        return f;
    }

    @Test
    void openProperExisting() throws IOException, SetupFailedException, KeyStoreException {
        final File f = newTempFile();
        final String pwd = UUID.randomUUID().toString();
        SecretProvider.keyStoreBased(KeyStoreHandler.create(f, KEYSTORE_PASSWORD.toCharArray()), "user"); // prep
        final Feature feature = new ZonkoidPasswordFeature(f, KEYSTORE_PASSWORD.toCharArray(), pwd.toCharArray());
        feature.setup();
        final SecretProvider s = SecretProvider.keyStoreBased(KeyStoreHandler.open(f, KEYSTORE_PASSWORD.toCharArray()));
        assertThat(s.getSecret(ZonkoidPasswordFeature.ZONKOID_ID)).contains(pwd.toCharArray());
    }

    @Test
    void createNewWithoutUsername() throws IOException {
        final File f = newTempFile();
        final String pwd = UUID.randomUUID().toString();
        final Feature feature = new ZonkoidPasswordFeature(f, KEYSTORE_PASSWORD.toCharArray(), pwd.toCharArray());
        assertThatThrownBy(feature::setup).isInstanceOf(SetupFailedException.class);
    }

    @Test
    void testFailsWithNonexistentProvider() throws IOException, KeyStoreException,
            SetupFailedException {
        final File f = newTempFile();
        final String pwd = UUID.randomUUID().toString();
        SecretProvider.keyStoreBased(KeyStoreHandler.create(f, KEYSTORE_PASSWORD.toCharArray()), "user"); // prep
        final Feature feature = new ZonkoidPasswordFeature("fakeId", f, KEYSTORE_PASSWORD.toCharArray(),
                                                           pwd.toCharArray());
        feature.setup();
        assertThatThrownBy(feature::test).isInstanceOf(TestFailedException.class); // fails due to non-existent provider
    }

    @Test
    void testFailsWithoutSetup() throws IOException {
        final File f = newTempFile();
        final String pwd = UUID.randomUUID().toString();
        final Feature feature = new ZonkoidPasswordFeature("fakeId", f, KEYSTORE_PASSWORD.toCharArray(),
                                                           pwd.toCharArray());
        assertThatThrownBy(feature::test).isInstanceOf(TestFailedException.class); // no setup performed
    }
}
