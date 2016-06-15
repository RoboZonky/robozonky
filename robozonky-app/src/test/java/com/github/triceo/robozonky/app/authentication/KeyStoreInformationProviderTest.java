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
import java.util.UUID;

import com.github.triceo.robozonky.app.util.KeyStoreHandler;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class KeyStoreInformationProviderTest {

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
            return KeyStoreHandler.create(f, UUID.randomUUID().toString());
        } catch (final IOException | KeyStoreException e) {
            Assertions.fail("Something went wrong.", e);
            return null;
        }
    }

    private static KeyStoreInformationProvider newProvider() {
        return new KeyStoreInformationProvider(KeyStoreInformationProviderTest.getKeyStoreHandler());
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
        final KeyStoreInformationProvider p = KeyStoreInformationProviderTest.newProvider();
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
        Assertions.assertThat(p.setToken(new StringReader(toStore))).isTrue();
        Assertions.assertThat(p.getToken()).isPresent();
        final String stored = IOUtils.toString(p.getToken().get());
        Assertions.assertThat(stored).isEqualTo(toStore);
        Assertions.assertThat(p.getTokenSetDate()).isPresent();
        final LocalDateTime storedOn = p.getTokenSetDate().get();
        Assertions.assertThat(storedOn).isAfter(beforeStoring);
        // clear token
        p.setToken();
        Assertions.assertThat(p.getToken()).isEmpty();
        Assertions.assertThat(p.getTokenSetDate()).isEmpty();
    }

}
