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
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyStoreException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import javax.xml.bind.JAXBException;

import com.github.triceo.robozonky.app.util.KeyStoreHandler;
import com.github.triceo.robozonky.authentication.Authenticator;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class AuthenticationHandlerTest {

    private static final ZonkyApiToken TOKEN = new ZonkyApiToken(UUID.randomUUID().toString(),
            UUID.randomUUID().toString(), 299, "A", "B");

    private static SecretProvider getNewProvider() throws IOException, KeyStoreException {
        final File file = File.createTempFile("robozonky-", ".keystore");
        file.delete();
        final String username = "user";
        final char[] password = "pass".toCharArray();
        final KeyStoreHandler ksh = KeyStoreHandler.create(file, password);
        return SecretProvider.keyStoreBased(ksh, username, password);
    }

    private static SecretProvider mockExistingProvider(final OffsetDateTime storedOn,
                                                       final boolean succeedInSavingTokens,
                                                       final boolean succeedInDeletingToken) throws JAXBException {
        return AuthenticationHandlerTest.mockExistingProvider(ZonkyApiToken.marshal(AuthenticationHandlerTest.TOKEN),
                storedOn, succeedInSavingTokens, succeedInDeletingToken);
    }

    private static SecretProvider mockExistingProvider(final String token, final OffsetDateTime storedOn,
                                                       final boolean succeedInSavingTokens,
                                                       final boolean succeedInDeletingToken) throws JAXBException {
        final Reader tokenReader = new StringReader(token);
        final SecretProvider p = Mockito.mock(SecretProvider.class);
        Mockito.when(p.getToken()).thenReturn(Optional.of(tokenReader));
        Mockito.when(p.getTokenSetDate()).thenReturn(Optional.of(storedOn));
        Mockito.when(p.setToken(Mockito.any())).thenReturn(succeedInSavingTokens);
        Mockito.when(p.deleteToken()).thenReturn(succeedInDeletingToken);
        return p;
    }

    @Test
    public void simplePasswordBased() throws IOException, KeyStoreException {
        final AuthenticationHandler h = AuthenticationHandler.passwordBased(
                AuthenticationHandlerTest.getNewProvider());
        final Authenticator a = h.build();
        Assertions.assertThat(a.isTokenBased()).isFalse();
        final boolean shouldLogout = h.isLogoutAllowed(Mockito.mock(ZonkyApiToken.class));
        Assertions.assertThat(shouldLogout).isTrue();
    }

    @Test
    public void simpleTokenBasedWithoutExistingToken() throws IOException, KeyStoreException {
        final AuthenticationHandler h = AuthenticationHandler.tokenBased(
                AuthenticationHandlerTest.getNewProvider());
        final Authenticator a = h.build();
        Assertions.assertThat(a.isTokenBased()).isFalse();
        final boolean shouldLogout = h.isLogoutAllowed(Mockito.mock(ZonkyApiToken.class));
        Assertions.assertThat(shouldLogout).isFalse();
    }

    @Test
    public void simpleTokenBasedWithExistingToken() throws JAXBException {
        final AuthenticationHandler h = AuthenticationHandler.tokenBased(
                AuthenticationHandlerTest.mockExistingProvider(OffsetDateTime.now(), true, true));
        final Authenticator a = h.build();
        Assertions.assertThat(a.isTokenBased()).isTrue();
        final boolean shouldLogout = h.isLogoutAllowed(Mockito.mock(ZonkyApiToken.class));
        Assertions.assertThat(shouldLogout).isFalse();
    }

    @Test
    public void tokenBasedWithExpiredToken() throws JAXBException {
        final OffsetDateTime expired =
                OffsetDateTime.now().minus(AuthenticationHandlerTest.TOKEN.getExpiresIn() + 1, ChronoUnit.SECONDS);
        final AuthenticationHandler h = AuthenticationHandler.tokenBased(
                AuthenticationHandlerTest.mockExistingProvider(expired, true, true));
        final Authenticator a = h.build();
        Assertions.assertThat(a.isTokenBased()).isFalse();
    }

    @Test
    public void tokenBasedWithExpiringToken() throws JAXBException {
        final OffsetDateTime expiring =
                OffsetDateTime.now().minus(AuthenticationHandlerTest.TOKEN.getExpiresIn() - 1, ChronoUnit.SECONDS);
        final AuthenticationHandler h = AuthenticationHandler.tokenBased(
                AuthenticationHandlerTest.mockExistingProvider(expiring, true, true), 10, ChronoUnit.SECONDS);
        final Authenticator a = h.build();
        Assertions.assertThat(a.isTokenBased()).isTrue();
    }

    @Test
    public void tokenBasedWithFailingToken() throws JAXBException {
        final SecretProvider provider =
                AuthenticationHandlerTest.mockExistingProvider("", OffsetDateTime.now(), true, true);
        final AuthenticationHandler h = AuthenticationHandler.tokenBased(provider, 10,
                ChronoUnit.SECONDS);
        final Authenticator a = h.build();
        Assertions.assertThat(a.isTokenBased()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void tokenBasedWithNoTokenToProcess() throws JAXBException {
        // prepare data
        final Reader tokenReader = new StringReader(ZonkyApiToken.marshal(AuthenticationHandlerTest.TOKEN));
        final SecretProvider p = Mockito.mock(SecretProvider.class);
        Mockito.when(p.getToken()).thenReturn(Optional.of(tokenReader));
        Mockito.when(p.getTokenSetDate()).thenReturn(Optional.of(OffsetDateTime.now()));
        final AuthenticationHandler h = AuthenticationHandler.tokenBased(p);
        final Authenticator a = h.build();
        Assertions.assertThat(a.isTokenBased()).isTrue();
        // now make sure token was deleted
        Mockito.when(p.getToken()).thenReturn(Optional.empty());
        Mockito.when(p.getToken()).thenReturn(Optional.empty());
        // make sure that when new token stored, logout not necessary
        Mockito.when(p.setToken(Mockito.any())).thenReturn(true);
        Assertions.assertThat(h.isLogoutAllowed(AuthenticationHandlerTest.TOKEN)).isFalse();
        // make sure that when new token not stored, logout forced
        Mockito.when(p.setToken(Mockito.any())).thenReturn(false);
        Assertions.assertThat(h.isLogoutAllowed(AuthenticationHandlerTest.TOKEN)).isTrue();
        // make sure when wrong token, logout forced
        Mockito.when(p.setToken(Mockito.any())).thenThrow(JAXBException.class); // instead of marshalling throwing
        Assertions.assertThat(h.isLogoutAllowed(Mockito.mock(ZonkyApiToken.class))).isTrue();
    }

}
