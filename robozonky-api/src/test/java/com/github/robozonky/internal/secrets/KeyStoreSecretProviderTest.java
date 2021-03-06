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

package com.github.robozonky.internal.secrets;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.internal.remote.entities.ZonkyApiTokenImpl;

class KeyStoreSecretProviderTest {

    private static final String USR = "username";
    private static final String PWD = "password";

    private static KeyStoreSecretProvider newMockProvider() {
        // make sure any query returns no value
        final KeyStoreHandler ksh = mock(KeyStoreHandler.class);
        when(ksh.get(any())).thenReturn(Optional.empty());
        return (KeyStoreSecretProvider) SecretProvider.keyStoreBased(ksh);
    }

    private static KeyStoreHandler getKeyStoreHandler() {
        try {
            final File f = File.createTempFile("robozonky-", ".keystore");
            f.delete();
            return KeyStoreHandler.create(f, PWD.toCharArray());
        } catch (final IOException | KeyStoreException e) {
            fail("Something went wrong.", e);
            return null;
        }
    }

    private static KeyStoreSecretProvider newProvider(final String username, final String password) {
        final KeyStoreHandler ksh = getKeyStoreHandler();
        return (KeyStoreSecretProvider) SecretProvider.keyStoreBased(ksh, username, password.toCharArray());
    }

    @Test
    void usernameNotSet() {
        assertThatThrownBy(() -> newMockProvider().getUsername())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void passwordNotSet() {
        assertThatThrownBy(() -> newMockProvider().getPassword())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void setUsernameAndPassword() {
        final KeyStoreSecretProvider p = newProvider(USR, PWD);
        // make sure original values were set
        assertThat(p.getUsername()).isEqualTo(USR);
        assertThat(p.getPassword()).isEqualTo(PWD.toCharArray());
        // make sure updating them works
        final String usr = "something";
        assertThat(p.setUsername(usr)).isTrue();
        assertThat(p.getUsername()).isEqualTo(usr);
        final String pwd = "somethingElse";
        assertThat(p.setPassword(pwd.toCharArray())).isTrue();
        assertThat(p.getPassword()).isEqualTo(pwd.toCharArray());
        assertThat(p.isPersistent()).isTrue();
    }

    @Test
    void setNullPassword() {
        final KeyStoreSecretProvider p = newProvider(USR, PWD);
        assertThat(p.setUsername(null)).isFalse();
    }

    @Test
    void setToken() {
        final SecretProvider p = newProvider(USR, PWD);
        // make sure original values were set
        assertThat(p.getToken()).isEmpty();
        final ZonkyApiToken token = new ZonkyApiTokenImpl(UUID.randomUUID()
            .toString(),
                UUID.randomUUID()
                    .toString(),
                299);
        assertThat(p.setToken(token)).isTrue();
        assertThat(p.getToken()).contains(token);
        assertThat(p.setToken(null)).isTrue();
        assertThat(p.getToken()).isEmpty();
    }

    @Test
    void setTokenIoFail() {
        final KeyStoreHandler ksh = spy(getKeyStoreHandler());
        doThrow(IllegalStateException.class).when(ksh)
            .save();
        doThrow(IllegalStateException.class).when(ksh)
            .get(any());
        final SecretProvider p = SecretProvider.keyStoreBased(ksh, UUID.randomUUID()
            .toString());
        final ZonkyApiToken token = new ZonkyApiTokenImpl(UUID.randomUUID()
            .toString(),
                UUID.randomUUID()
                    .toString(),
                299);
        assertThat(p.setToken(token)).isFalse();
        assertThat(p.getToken()).isEmpty();
        assertThat(p.setToken(null)).isFalse();
        assertThat(p.getToken()).isEmpty();
    }

    @Test
    void noKeyStoreHandlerProvided() {
        assertThatThrownBy(() -> new KeyStoreSecretProvider(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
