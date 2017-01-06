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

    @Test
    public void wrongFormatKeyStoreProvided() throws Exception {
        final File tmp = File.createTempFile("robozonky-", ".keystore");
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getPassword()).thenReturn("password".toCharArray());
        Mockito.when(cli.getKeyStoreLocation()).thenReturn(Optional.of(tmp));
        Assertions.assertThat(SecretProviderFactory.newSecretProvider(cli, null)).isEmpty();
    }

    @Test
    public void failedDeletingKeyStore() throws Exception {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getKeyStoreLocation()).thenReturn(Optional.empty());
        final File f = Mockito.mock(File.class);
        Mockito.when(f.canRead()).thenReturn(true);
        Mockito.when(f.delete()).thenReturn(false);
        Assertions.assertThat(SecretProviderFactory.newSecretProvider(cli, f)).isEmpty();
    }

    @Test
    public void noKeyStoreNoUsername() throws Exception {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getKeyStoreLocation()).thenReturn(Optional.empty());
        Mockito.when(cli.getUsername()).thenReturn(Optional.empty());
        final File f = Mockito.mock(File.class);
        Mockito.when(f.canRead()).thenReturn(false);
        Assertions.assertThat(SecretProviderFactory.newSecretProvider(cli, f)).isEmpty();
    }

    @Test
    public void fallbackDemandingKeystore() {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getKeyStoreLocation()).thenReturn(Optional.of(new File("")));
        Assertions.assertThat(SecretProviderFactory.getFallbackSecretProvider(cli)).isEmpty();
    }

    @Test
    public void fallbackSuccess() {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getKeyStoreLocation()).thenReturn(Optional.empty());
        Mockito.when(cli.getUsername()).thenReturn(Optional.of("user"));
        Mockito.when(cli.getPassword()).thenReturn("pass".toCharArray());
        Assertions.assertThat(SecretProviderFactory.getFallbackSecretProvider(cli)).isNotEmpty();
    }

}
