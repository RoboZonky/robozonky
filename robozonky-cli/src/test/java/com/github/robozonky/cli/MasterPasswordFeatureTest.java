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

package com.github.robozonky.cli;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;

import com.github.robozonky.common.secrets.KeyStoreHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MasterPasswordFeatureTest {

    private static final String ORIG_PASSWORD = "pwd";
    private static final String NEW_PASSWORD = "pwd2";

    private static File newTempFile() throws IOException {
        final File f = File.createTempFile("robozonky-", ".keystore");
        f.delete();
        return f;
    }

    @Test
    void createNew() throws IOException, SetupFailedException, KeyStoreException {
        final File f = newTempFile();
        final Feature feature = new MasterPasswordFeature(f, ORIG_PASSWORD.toCharArray(), NEW_PASSWORD.toCharArray());
        feature.setup();
        assertThat(f).exists();
        KeyStoreHandler.open(f, NEW_PASSWORD.toCharArray()); // throws if keystore cannot be opened with new password
    }

    @Test
    void reuseWithWrongPassword() throws IOException, KeyStoreException {
        final File f = newTempFile();
        KeyStoreHandler.create(f, ORIG_PASSWORD.toCharArray());
        final Feature feature = new MasterPasswordFeature(f, NEW_PASSWORD.toCharArray()); // open with wrong password
        assertThatThrownBy(feature::setup).isInstanceOf(SetupFailedException.class);
    }

    @Test
    void reuse() throws IOException, SetupFailedException, KeyStoreException {
        final File f = newTempFile();
        KeyStoreHandler.create(f, ORIG_PASSWORD.toCharArray());
        final Feature feature = new MasterPasswordFeature(f, ORIG_PASSWORD.toCharArray(), NEW_PASSWORD.toCharArray());
        feature.setup();
        KeyStoreHandler.open(f, NEW_PASSWORD.toCharArray()); // throws if keystore cannot be opened with new password
    }

    @Test
    void passwordFailWhenTesting() throws IOException, KeyStoreException {
        final File f = newTempFile();
        KeyStoreHandler.create(f, ORIG_PASSWORD.toCharArray());
        final Feature feature = new MasterPasswordFeature(f, ORIG_PASSWORD.toCharArray(), NEW_PASSWORD.toCharArray());
        assertThatThrownBy(feature::test).isInstanceOf(TestFailedException.class);
    }
}
