/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.configuration;

import java.io.File;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class SecretProviderFactoryTest {

    private static CommandLine mockCli(final String username, final File file, final char... password) {
        final AuthenticationCommandLineFragment delegate = Mockito.mock(AuthenticationCommandLineFragment.class);
        Mockito.when(delegate.getUsername()).thenReturn(Optional.ofNullable(username));
        Mockito.when(delegate.getKeystore()).thenReturn(Optional.ofNullable(file));
        Mockito.when(delegate.getPassword()).thenReturn(password);
        final CommandLine cli = Mockito.mock(CommandLine.class);
        Mockito.when(cli.getAuthenticationFragment()).thenReturn(delegate);
        return cli;
    }

    @Test
    public void wrongFormatKeyStoreProvided() throws Exception {
        final File tmp = File.createTempFile("robozonky-", ".keystore");
        final CommandLine cli = SecretProviderFactoryTest.mockCli(null, tmp, "password".toCharArray());
        Assertions.assertThat(SecretProviderFactory.getSecretProvider(cli)).isEmpty();
    }

    @Test
    public void fallbackDemandingKeystore() {
        final CommandLine cli = SecretProviderFactoryTest.mockCli(null, new File(""));
        Assertions.assertThat(SecretProviderFactory.getFallbackSecretProvider(cli)).isEmpty();
    }

    @Test
    public void fallbackSuccess() {
        final CommandLine cli = SecretProviderFactoryTest.mockCli("user", null, "pass".toCharArray());
        Assertions.assertThat(SecretProviderFactory.getFallbackSecretProvider(cli)).isNotEmpty();
    }
}
