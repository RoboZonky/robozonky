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
import java.security.KeyStoreException;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class SecretProviderFactoryTest {

    private static AuthenticationCommandLineFragment mockCli(final String username, final File file,
                                                             final char... password) {
        final AuthenticationCommandLineFragment delegate = Mockito.mock(AuthenticationCommandLineFragment.class);
        Mockito.when(delegate.getUsername()).thenReturn(Optional.ofNullable(username));
        Mockito.when(delegate.getKeystore()).thenReturn(Optional.ofNullable(file));
        Mockito.when(delegate.getPassword()).thenReturn(password);
        return delegate;
    }

    @Test
    public void nonexistentKeyStoreProvided() throws Exception {
        final File tmp = File.createTempFile("robozonky-", ".keystore");
        tmp.delete();
        final AuthenticationCommandLineFragment cli = SecretProviderFactoryTest.mockCli(null, tmp,
                                                                                        "password".toCharArray());
        Assertions.assertThat(SecretProviderFactory.getSecretProvider(cli)).isEmpty();
    }

    @Test
    public void wrongFormatKeyStoreProvided() throws Exception {
        final File tmp = Mockito.mock(File.class);
        Mockito.doThrow(KeyStoreException.class).when(tmp).exists();
        final AuthenticationCommandLineFragment cli = SecretProviderFactoryTest.mockCli(null, tmp,
                                                                                        "password".toCharArray());
        Assertions.assertThat(SecretProviderFactory.getSecretProvider(cli)).isEmpty();
    }

    @Test
    public void fallbackSuccess() {
        final String username = "user", password = "pass";
        final AuthenticationCommandLineFragment cli = SecretProviderFactoryTest.mockCli(username, null,
                                                                                        password.toCharArray());
        Assertions.assertThat(SecretProviderFactory.getSecretProvider(cli))
                .hasValueSatisfying(secret -> SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(secret.getUsername()).isEqualTo(username);
                    softly.assertThat(secret.getPassword()).containsExactly(password.toCharArray());
                    softly.assertThat(secret.isPersistent()).isFalse();
                }));
    }
}
