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

package com.github.robozonky.app.configuration;

import java.io.File;
import java.util.Optional;

import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecretProviderFactoryTest extends AbstractRoboZonkyTest {

    private static CommandLine mockCli(final File file, final char... password) {
        final CommandLine delegate = mock(CommandLine.class);
        when(delegate.getKeystore()).thenReturn(Optional.ofNullable(file));
        when(delegate.getPassword()).thenReturn(password);
        return delegate;
    }

    @Test
    void nonexistentKeyStoreProvided() throws Exception {
        final File tmp = File.createTempFile("robozonky-", ".keystore");
        tmp.delete();
        final CommandLine cli = SecretProviderFactoryTest.mockCli(tmp, "password".toCharArray());
        assertThat(SecretProviderFactory.getSecretProvider(cli)).isEmpty();
    }

    @Test
    void wrongFormatKeyStoreProvided() throws Exception {
        final File tmp = File.createTempFile("robozonky-", ".keystore"); // empty key store
        final CommandLine cli = SecretProviderFactoryTest.mockCli(tmp, "password".toCharArray());
        assertThat(SecretProviderFactory.getSecretProvider(cli)).isEmpty();
    }

    @Test
    void noFallback() {
        final String password = "pass";
        final CommandLine cli = SecretProviderFactoryTest.mockCli(null, password.toCharArray());
        assertThatThrownBy(() -> SecretProviderFactory.getSecretProvider(cli))
                .isInstanceOf(IllegalStateException.class);
    }
}
