/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStoreException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.app.daemon.Daemon;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.internal.extensions.ListenerServiceLoader;
import com.github.robozonky.internal.secrets.KeyStoreHandler;
import com.github.robozonky.internal.secrets.SecretProvider;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandLineTest extends AbstractRoboZonkyTest {

    private static App mockedApp(final String... args) {
        final App main = spy(new App(args));
        doNothing().when(main).actuallyExit(anyInt());
        return main;
    }

    @BeforeAll
    static void ensureSecurityWorks() {
        /*
         * poorly configured JVMs in remote CI systems will fail here due to
         * https://stackoverflow.com/questions/27036588/errorcould-not-initialize-class-javax-crypto-jcesecurity
         */
        Assumptions.assumeThatCode(() -> {
            final String keyStorePassword = "password";
            final File keystore = File.createTempFile("robozonky-", ".keystore");
            keystore.delete();
            KeyStoreHandler.create(keystore, keyStorePassword.toCharArray());
        }).doesNotThrowAnyException();
    }

    private static Path getPath(final InputStream resource) throws IOException {
        final File f = File.createTempFile("robozonky-", ".tmp");
        f.delete();
        final Path p = f.toPath();
        Files.copy(resource, p);
        return p;
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
        final App main = new App("-n", name, "-g", keystore.getAbsolutePath(), "-p", keyStorePassword, "-i",
                                 "somewhere.txt",
                                 "-s", "somewhere");
        final Optional<Function<Lifecycle, InvestmentMode>> cfg = CommandLine.parse(main);
        assertThat(cfg).isPresent();
        assertThat(cfg.get().apply(null)).isInstanceOf(Daemon.class);
        assertThat(ListenerServiceLoader.getNotificationConfiguration(new SessionInfo(username))).isNotEmpty();
    }

    @Test
    void validDaemonCliNoKeystore() {
        final App main = mockedApp("-g", "a", "-p", "p", "-i", "somewhere.txt", "-s", "somewhere");
        final Optional<Function<Lifecycle, InvestmentMode>> cfg = CommandLine.parse(main);
        assertThat(cfg).isEmpty();
    }

    @Test
    void quotedAtFile() throws IOException {
        final CommandLine cli = new CommandLine();
        picocli.CommandLine.call(cli, "@" + getPath(getClass().getResourceAsStream("quoted.cli")));
        assertThat(cli.getKeystore()).contains(new File("C:\\Program Files\\RoboZonky\\robozonky.keystore"));
        assertThat(cli.getName()).isEqualTo("Testing Name");
    }

    @Test
    void unquotedAtFile() throws IOException {
        final CommandLine cli = new CommandLine();
        picocli.CommandLine.call(cli, "@" + getPath(getClass().getResourceAsStream("unquoted.cli")));
        assertThat(cli.getKeystore()).contains(new File("C:\\Program Files\\RoboZonky\\robozonky.keystore"));
        assertThat(cli.getName()).isEqualTo("Testing Name");
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

    @Test
    void getters() {
        final CommandLine cli = new CommandLine();
        assertThat(cli.getKeystore()).isEmpty();
        assertThat(cli.getNotificationConfigLocation()).isEmpty();
        assertThat(cli.getName()).isEqualTo("Unnamed");
    }
}
