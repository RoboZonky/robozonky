/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class AuthenticationHandlerProviderTest {

    @Test
    public void wrongFormatKeyStoreProvided() throws IOException {
        final File tmp = File.createTempFile("robozonky-", ".keystore");
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getPassword()).thenReturn("password");
        Mockito.when(cli.getKeyStoreLocation()).thenReturn(Optional.of(tmp));
        Assertions.assertThat(AuthenticationHandlerProvider.getSecretProvider(cli, null)).isEmpty();
    }

    @Test
    public void failedDeletingKeyStore() throws IOException {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getKeyStoreLocation()).thenReturn(Optional.empty());
        final File f = Mockito.mock(File.class);
        Mockito.when(f.canRead()).thenReturn(true);
        Mockito.when(f.delete()).thenReturn(false);
        Assertions.assertThat(AuthenticationHandlerProvider.getSecretProvider(cli, f)).isEmpty();
    }

    @Test
    public void noKeyStoreNoUsername() throws IOException {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getKeyStoreLocation()).thenReturn(Optional.empty());
        Mockito.when(cli.getUsername()).thenReturn(Optional.empty());
        final File f = Mockito.mock(File.class);
        Mockito.when(f.canRead()).thenReturn(false);
        Assertions.assertThat(AuthenticationHandlerProvider.getSecretProvider(cli, f)).isEmpty();
    }

    @Test
    public void authenticationHandlerWithoutToken() {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.isTokenEnabled()).thenReturn(false);
        final AuthenticationHandler a = AuthenticationHandlerProvider.instantiateAuthenticationHandler(null, cli);
        Assertions.assertThat(a.isTokenBased()).isFalse();
    }

    @Test
    public void authenticationHandlerWithTokenAndNoExpiration() {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.isTokenEnabled()).thenReturn(true);
        Mockito.when(cli.getTokenRefreshBeforeExpirationInSeconds()).thenReturn(Optional.empty());
        final AuthenticationHandler a = AuthenticationHandlerProvider.instantiateAuthenticationHandler(null, cli);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(a.isTokenBased()).isTrue();
        softly.assertThat(a.getTokenRefreshBeforeExpirationInSeconds()).isEqualTo(60);
        softly.assertAll();
    }

    @Test
    public void authenticationHandlerWithTokenAndExpiration() {
        final int expiration = 120;
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.isTokenEnabled()).thenReturn(true);
        Mockito.when(cli.getTokenRefreshBeforeExpirationInSeconds()).thenReturn(Optional.of(expiration));
        final AuthenticationHandler a = AuthenticationHandlerProvider.instantiateAuthenticationHandler(null, cli);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(a.isTokenBased()).isTrue();
        softly.assertThat(a.getTokenRefreshBeforeExpirationInSeconds()).isEqualTo(expiration);
        softly.assertAll();
    }

    private static final File returnTempFile() throws IOException {
        final File f = File.createTempFile("robozonky-", ".keystore");
        f.delete();
        return f;
    }

    @Test
    public void usernameMissing() throws IOException {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getKeyStoreLocation())
                .thenReturn(Optional.of(AuthenticationHandlerProviderTest.returnTempFile()));
        Mockito.when(cli.getUsername()).thenReturn(Optional.empty());
        final AuthenticationHandlerProvider ahp = new AuthenticationHandlerProvider();
        Assertions.assertThat(ahp.apply(cli)).isEmpty();
    }

    @Test
    public void fileNonexistent() throws IOException {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getKeyStoreLocation())
                .thenReturn(Optional.of(AuthenticationHandlerProviderTest.returnTempFile()));
        Mockito.when(cli.getUsername()).thenReturn(Optional.of("username"));
        Mockito.when(cli.getPassword()).thenReturn("password");
        final AuthenticationHandlerProvider ahp = new AuthenticationHandlerProvider();
        Assertions.assertThat(ahp.apply(cli)).isEmpty();
    }

    @Test
    public void correctCreationOfNew() throws IOException {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getKeyStoreLocation()).thenReturn(Optional.empty());
        Mockito.when(cli.getUsername()).thenReturn(Optional.of("username"));
        Mockito.when(cli.getPassword()).thenReturn("password");
        final AuthenticationHandlerProvider ahp = new AuthenticationHandlerProvider();
        Assertions.assertThat(ahp.apply(cli)).isPresent();
    }

}
