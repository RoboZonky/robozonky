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

package com.github.robozonky.app.configuration;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Optional;
import java.util.UUID;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.app.App;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.common.extensions.ListenerServiceLoader;
import com.github.robozonky.common.secrets.KeyStoreHandler;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class CommandLineTest extends AbstractRoboZonkyTest {

    private static App mockedApp(final String... args) {
        final App main = spy(new App(args));
        doNothing().when(main).actuallyExit(anyInt());
        return main;
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
        final String name = UUID.randomUUID().toString();
        final App main = new App("-n", name, "-g", keystore.getAbsolutePath(), "-p", keyStorePassword, "-i", "somewhere.txt",
                                 "-s", "somewhere");
        final Optional<InvestmentMode> cfg = CommandLine.parse(main);
        assertThat(cfg).isPresent();
        assertThat(cfg.get().getSessionName()).isEqualTo(name);
        assertThat(ListenerServiceLoader.getNotificationConfiguration(new SessionInfo(username))).isNotEmpty();
    }

    @Test
    void validDaemonCliNoKeystore() {
        final App main = mockedApp("-g", "a", "-p", "p", "-i", "somewhere.txt", "-s", "somewhere");
        final Optional<InvestmentMode> cfg = CommandLine.parse(main);
        assertThat(cfg).isEmpty();
    }

    @Test
    void helpCli() {
        final App main = mockedApp("-h");
        main.run();
        verify(main).actuallyExit(eq(ReturnCode.ERROR_SETUP.getCode()));
    }

    @Test
    void invalidCli() {
        final App main = mockedApp();
        main.run();
        verify(main).actuallyExit(eq(ReturnCode.ERROR_SETUP.getCode()));
    }
}
