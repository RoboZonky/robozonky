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
import org.mockito.Matchers;
import org.mockito.Mockito;

public class KeyStoreInformationProviderTest {

    private static String USR = "username";
    private static String PWD = "password";

    private static KeyStoreInformationProvider newMockProvider() {
        // make sure any query returns no value
        final KeyStoreHandler ksh = Mockito.mock(KeyStoreHandler.class);
        Mockito.when(ksh.get(Mockito.any())).thenReturn(Optional.empty());
        return (KeyStoreInformationProvider)SensitiveInformationProvider.keyStoreBased(ksh);
    }

    private static KeyStoreHandler getKeyStoreHandler() {
        try {
            final File f = File.createTempFile("robozonky-", ".keystore");
            f.delete();
            return KeyStoreHandler.create(f, KeyStoreInformationProviderTest.PWD);
        } catch (final IOException | KeyStoreException e) {
            Assertions.fail("Something went wrong.", e);
            return null;
        }
    }

    private static KeyStoreInformationProvider newProvider() {
        final KeyStoreHandler ksh = KeyStoreInformationProviderTest.getKeyStoreHandler();
        return (KeyStoreInformationProvider)SensitiveInformationProvider.keyStoreBased(ksh);
    }

    private static KeyStoreInformationProvider newProvider(final String username, final String password) {
        final KeyStoreHandler ksh = KeyStoreInformationProviderTest.getKeyStoreHandler();
        return (KeyStoreInformationProvider)SensitiveInformationProvider.keyStoreBased(ksh, username, password);
    }

    @Test(expected = IllegalStateException.class)
    public void usernameNotSet() {
        KeyStoreInformationProviderTest.newMockProvider().getUsername();
    }

    @Test(expected = IllegalStateException.class)
    public void passwordNotSet() {
        KeyStoreInformationProviderTest.newMockProvider().getPassword();
    }

    @Test
    public void tokenNotSet() {
        final KeyStoreInformationProvider p = KeyStoreInformationProviderTest.newMockProvider();
        Assertions.assertThat(p.getToken()).isEmpty();
        Assertions.assertThat(p.getTokenSetDate()).isEmpty();
    }

    @Test
    public void setUsernameAndPassword() {
        final KeyStoreInformationProvider p =
                KeyStoreInformationProviderTest.newProvider(KeyStoreInformationProviderTest.USR,
                        KeyStoreInformationProviderTest.PWD);
        // make sure original values were set
        Assertions.assertThat(p.getUsername()).isEqualTo(KeyStoreInformationProviderTest.USR);
        Assertions.assertThat(p.getPassword()).isEqualTo(KeyStoreInformationProviderTest.PWD);
        // make sure updating them works
        final String usr = "something";
        p.setUsername(usr);
        Assertions.assertThat(p.getUsername()).isEqualTo(usr);
        final String pwd = "somethingElse";
        p.setPassword(pwd);
        Assertions.assertThat(p.getPassword()).isEqualTo(pwd);
    }

    @Test
    public void tokenManipulation() throws IOException {
        final KeyStoreInformationProvider p = KeyStoreInformationProviderTest.newProvider();
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
        final KeyStoreInformationProvider p = new KeyStoreInformationProvider(ksh);
        Assertions.assertThat(p.deleteToken()).isFalse();
        Mockito.verify(ksh, Mockito.times(1)).save();
        Mockito.verify(ksh, Mockito.times(2)).delete(Mockito.any());
    }

    @Test
    public void tokenDeleteSucceeded() throws IOException {
        final KeyStoreHandler ksh = Mockito.mock(KeyStoreHandler.class);
        Mockito.doReturn(true).when(ksh).delete(Matchers.any());
        final KeyStoreInformationProvider p = new KeyStoreInformationProvider(ksh);
        Assertions.assertThat(p.deleteToken()).isTrue();
        Mockito.verify(ksh, Mockito.times(1)).save();
        Mockito.verify(ksh, Mockito.times(2)).delete(Mockito.any());
    }

    @Test
    public void tokenSaveFailed() throws IOException {
        final KeyStoreHandler ksh = Mockito.mock(KeyStoreHandler.class);
        Mockito.doThrow(IOException.class).when(ksh).save();
        final KeyStoreInformationProvider p = new KeyStoreInformationProvider(ksh);
        Assertions.assertThat(p.setToken(new StringReader("something"))).isFalse();
        Mockito.verify(ksh, Mockito.atLeast(1)).save();
        Mockito.verify(ksh, Mockito.times(2)).set(Mockito.any(), Mockito.any(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void noKeyStoreHandlerProvided() {
        new KeyStoreInformationProvider(null);
    }

}
