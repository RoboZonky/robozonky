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

package com.github.triceo.robozonky.app.authentication;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyStoreException;
import java.time.LocalDateTime;
import java.util.Optional;

import com.github.triceo.robozonky.app.util.IOUtils;
import com.github.triceo.robozonky.app.util.KeyStoreHandler;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class KeyStoreSecretProviderTest {

    private static final String USR = "username";
    private static final String PWD = "password";

    private static KeyStoreSecretProvider newMockProvider() {
        // make sure any query returns no value
        final KeyStoreHandler ksh = Mockito.mock(KeyStoreHandler.class);
        Mockito.when(ksh.get(Mockito.any())).thenReturn(Optional.empty());
        return (KeyStoreSecretProvider) SecretProvider.keyStoreBased(ksh);
    }

    private static KeyStoreHandler getKeyStoreHandler() {
        try {
            final File f = File.createTempFile("robozonky-", ".keystore");
            f.delete();
            return KeyStoreHandler.create(f, KeyStoreSecretProviderTest.PWD.toCharArray());
        } catch (final IOException | KeyStoreException e) {
            Assertions.fail("Something went wrong.", e);
            return null;
        }
    }

    private static KeyStoreSecretProvider newProvider() {
        final KeyStoreHandler ksh = KeyStoreSecretProviderTest.getKeyStoreHandler();
        return (KeyStoreSecretProvider) SecretProvider.keyStoreBased(ksh);
    }

    private static KeyStoreSecretProvider newProvider(final String username, final String password) {
        final KeyStoreHandler ksh = KeyStoreSecretProviderTest.getKeyStoreHandler();
        return (KeyStoreSecretProvider) SecretProvider.keyStoreBased(ksh, username, password.toCharArray());
    }

    @Test(expected = IllegalStateException.class)
    public void usernameNotSet() {
        KeyStoreSecretProviderTest.newMockProvider().getUsername();
    }

    @Test(expected = IllegalStateException.class)
    public void passwordNotSet() {
        KeyStoreSecretProviderTest.newMockProvider().getPassword();
    }

    @Test
    public void tokenNotSet() {
        final KeyStoreSecretProvider p = KeyStoreSecretProviderTest.newMockProvider();
        Assertions.assertThat(p.getToken()).isEmpty();
        Assertions.assertThat(p.getTokenSetDate()).isEmpty();
    }

    @Test
    public void setUsernameAndPassword() {
        final KeyStoreSecretProvider p =
                KeyStoreSecretProviderTest.newProvider(KeyStoreSecretProviderTest.USR, KeyStoreSecretProviderTest.PWD);
        // make sure original values were set
        Assertions.assertThat(p.getUsername()).isEqualTo(KeyStoreSecretProviderTest.USR);
        Assertions.assertThat(p.getPassword()).isEqualTo(KeyStoreSecretProviderTest.PWD.toCharArray());
        // make sure updating them works
        final String usr = "something";
        Assertions.assertThat(p.setUsername(usr)).isTrue();
        Assertions.assertThat(p.getUsername()).isEqualTo(usr);
        final String pwd = "somethingElse";
        Assertions.assertThat(p.setPassword(pwd.toCharArray())).isTrue();
        Assertions.assertThat(p.getPassword()).isEqualTo(pwd.toCharArray());
    }

    @Test
    public void tokenManipulation() throws IOException {
        final KeyStoreSecretProvider p = KeyStoreSecretProviderTest.newProvider();
        final String toStore = "something";
        // store token
        final LocalDateTime beforeStoring = LocalDateTime.now();
        try {
            // makes sure the following code is always executed on a later timestamp than the previous code
            Thread.sleep(1);
        } catch (final InterruptedException ex) {
            // do nothing
        }
        Assertions.assertThat(p.setToken(new StringReader(toStore))).isTrue();
        Assertions.assertThat(p.getToken()).isPresent();
        final String stored = IOUtils.toString(p.getToken().get());
        Assertions.assertThat(stored).isEqualTo(toStore);
        Assertions.assertThat(p.getTokenSetDate()).isPresent();
        final LocalDateTime storedOn = p.getTokenSetDate().get();
        Assertions.assertThat(storedOn).isAfter(beforeStoring);
        // clear token
        p.deleteToken();
        Assertions.assertThat(p.getToken()).isEmpty();
        Assertions.assertThat(p.getTokenSetDate()).isEmpty();
    }

    @Test
    public void tokenDeleteFailed() throws IOException {
        final KeyStoreHandler ksh = Mockito.mock(KeyStoreHandler.class);
        Mockito.doThrow(IOException.class).when(ksh).save();
        final KeyStoreSecretProvider p = new KeyStoreSecretProvider(ksh);
        Assertions.assertThat(p.deleteToken()).isFalse();
        Mockito.verify(ksh, Mockito.times(1)).save();
        Mockito.verify(ksh, Mockito.times(2)).delete(Mockito.any());
    }

    @Test
    public void tokenDeleteSucceeded() throws IOException {
        final KeyStoreHandler ksh = Mockito.mock(KeyStoreHandler.class);
        Mockito.doReturn(true).when(ksh).delete(Mockito.any());
        final KeyStoreSecretProvider p = new KeyStoreSecretProvider(ksh);
        Assertions.assertThat(p.deleteToken()).isTrue();
        Mockito.verify(ksh, Mockito.times(1)).save();
        Mockito.verify(ksh, Mockito.times(2)).delete(Mockito.any());
    }

    @Test
    public void tokenSaveFailed() throws IOException {
        final KeyStoreHandler ksh = Mockito.mock(KeyStoreHandler.class);
        Mockito.doThrow(IOException.class).when(ksh).save();
        final KeyStoreSecretProvider p = new KeyStoreSecretProvider(ksh);
        Assertions.assertThat(p.setToken(new StringReader("something"))).isFalse();
        Mockito.verify(ksh, Mockito.atLeast(1)).save();
        Mockito.verify(ksh, Mockito.times(2)).set(Mockito.any(), Mockito.any(char[].class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void noKeyStoreHandlerProvided() {
        new KeyStoreSecretProvider(null);
    }

}
