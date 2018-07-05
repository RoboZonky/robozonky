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

package com.github.robozonky.app.configuration;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Optional;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.common.extensions.ListenerServiceLoader;
import com.github.robozonky.common.secrets.KeyStoreHandler;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandLineTest extends AbstractRoboZonkyTest {

    @Test
    void properScriptIdentification() {
        System.setProperty("os.name", "Some Windows System");
        assertThat(CommandLine.getScriptIdentifier()).isEqualTo("robozonky.bat");
        System.setProperty("os.name", "Any Other System");
        assertThat(CommandLine.getScriptIdentifier()).isEqualTo("robozonky.sh");
    }

    @Test
    void validDaemonCli() throws IOException, KeyStoreException {
        // prepare keystore
        final String keyStorePassword = "password";
        final File keystore = File.createTempFile("robozonky-", ".keystore");
        keystore.delete();
        final KeyStoreHandler ksh = KeyStoreHandler.create(keystore, keyStorePassword.toCharArray());
        final String username = "someone@somewhere.cz";
        SecretProvider.keyStoreBased(ksh, username, "something".toCharArray());
        // run the app
        final Optional<InvestmentMode> cfg = CommandLine.parse((t) -> {
                                                               }, "-g", keystore.getAbsolutePath(), "-p",
                                                               keyStorePassword, "-i", "somewhere.txt", "-s",
                                                               "somewhere");
        assertThat(cfg).isPresent();
        assertThat(ListenerServiceLoader.getNotificationConfiguration(new SessionInfo(username))).isNotEmpty();
    }

    @Test
    void helpCli() {
        final Optional<InvestmentMode> cfg = CommandLine.parse((t) -> {
        }, "-h");
        assertThat(cfg).isEmpty(); // would have called System.exit(), but we prevented that
    }

    @Test
    void invalidCli() {
        final Optional<InvestmentMode> cfg = CommandLine.parse((t) -> {
        });
        assertThat(cfg).isEmpty(); // would have called System.exit(), but we prevented that
    }
}
