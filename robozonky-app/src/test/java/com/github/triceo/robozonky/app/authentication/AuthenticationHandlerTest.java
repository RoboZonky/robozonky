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

package com.github.triceo.robozonky.app.authentication;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyStoreException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;
import javax.xml.bind.JAXBException;

import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.common.AbstractStateLeveragingTest;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.github.triceo.robozonky.common.remote.OAuth;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AuthenticationHandlerTest extends AbstractStateLeveragingTest {

    private static SecretProvider getNewProvider() {
        final String username = "user";
        final char[] password = "pass".toCharArray();
        final SecretProvider p = SecretProvider.fallback(username, password);
        p.deleteToken(); // just making sure the internal state of the fallback provider does not interfere
        return p;
    }

    private static SecretProvider getNewProvider(final OffsetDateTime storedOn) throws JAXBException {
        final ZonkyApiToken t = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), storedOn);
        return AuthenticationHandlerTest.getNewProvider(ZonkyApiToken.marshal(t));
    }

    private static SecretProvider getNewProvider(final String token) {
        final SecretProvider p = AuthenticationHandlerTest.getNewProvider();
        p.setToken(new StringReader(token));
        return p;
    }

    @Test
    public void simplePasswordBased() {
        final SecretProvider secrets = AuthenticationHandlerTest.getNewProvider();
        final ApiProvider apiProvider = Mockito.spy(new ApiProvider());
        final OAuth zonkyOauth = Mockito.mock(OAuth.class);
        Mockito.doReturn(AuthenticatorTest.TOKEN).when(zonkyOauth)
                .login(ArgumentMatchers.anyString(), ArgumentMatchers.any());
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.doReturn(zonkyOauth).when(apiProvider).oauth();
        Mockito.doReturn(z).when(apiProvider).authenticated(Mockito.eq(AuthenticatorTest.TOKEN));
        final AuthenticationHandler auth = AuthenticationHandler.passwordBased(secrets);
        Assertions.assertThat(auth.getSecretProvider()).isSameAs(secrets);
        auth.setApiProvider(apiProvider);
        Assertions.assertThat(auth.execute((a) -> Collections.emptyList())).isEmpty();
        Mockito.verify(zonkyOauth, Mockito.times(1))
                .login(ArgumentMatchers.eq(secrets.getUsername()),
                        ArgumentMatchers.eq(secrets.getPassword()));
        Mockito.verify(zonkyOauth, Mockito.never()).refresh(ArgumentMatchers.any());
        Mockito.verify(z, Mockito.times(1)).logout();
    }

    @Test
    public void simplePasswordBasedNoop() {
        final SecretProvider secrets = AuthenticationHandlerTest.getNewProvider();
        final ApiProvider apiProvider = Mockito.spy(new ApiProvider());
        final AuthenticationHandler auth = AuthenticationHandler.passwordBased(secrets);
        auth.setApiProvider(apiProvider);
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.doReturn(Mockito.mock(OAuth.class)).when(apiProvider).oauth();
        Mockito.doReturn(z).when(apiProvider).authenticated(Mockito.eq(AuthenticatorTest.TOKEN));
        Assertions.assertThat(auth.getSecretProvider()).isSameAs(secrets);
        Assertions.assertThat(auth.execute(null)).isEmpty();
    }

    @Test
    public void simpleTokenBasedWithExistingToken() throws JAXBException {
        final SecretProvider secretProvider = AuthenticationHandlerTest.getNewProvider(OffsetDateTime.now());
        final AuthenticationHandler auth = AuthenticationHandler.tokenBased(secretProvider, Duration.ofSeconds(60));
        final ApiProvider apiProvider = Mockito.spy(new ApiProvider());
        auth.setApiProvider(apiProvider);
        final OAuth zonkyOauth = Mockito.mock(OAuth.class);
        Mockito.doReturn(zonkyOauth).when(apiProvider).oauth();
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.doReturn(z).when(apiProvider).authenticated(Mockito.eq(AuthenticatorTest.TOKEN));
        Assertions.assertThat(auth.execute((a) -> Collections.emptyList())).isEmpty();
        Mockito.verify(zonkyOauth, Mockito.never()).refresh(ArgumentMatchers.any());
        Mockito.verify(zonkyOauth, Mockito.never()).login(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(z, Mockito.never()).logout();
    }

    @Test
    public void simpleTokenBasedWithExistingTokenNoop() throws JAXBException {
        final SecretProvider secretProvider = AuthenticationHandlerTest.getNewProvider(OffsetDateTime.now());
        final ApiProvider apiProvider = Mockito.mock(ApiProvider.class);
        final AuthenticationHandler h = AuthenticationHandler.tokenBased(secretProvider, Duration.ofSeconds(60));
        h.setApiProvider(apiProvider);
        Mockito.when(apiProvider.oauth()).thenReturn(Mockito.mock(OAuth.class));
        Assertions.assertThat(h.execute(null)).isEmpty();
    }

    @Test
    public void simpleTokenBasedWithoutToken() throws JAXBException, IOException, KeyStoreException {
        final OAuth zonkyOauth = Mockito.mock(OAuth.class);
        Mockito.doReturn(AuthenticatorTest.TOKEN).when(zonkyOauth)
                .login(ArgumentMatchers.anyString(), ArgumentMatchers.any());
        final ApiProvider apiProvider = Mockito.spy(new ApiProvider());
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.doReturn(zonkyOauth).when(apiProvider).oauth();
        Mockito.doReturn(z).when(apiProvider).authenticated(Mockito.eq(AuthenticatorTest.TOKEN));
        final SecretProvider secrets = AuthenticationHandlerTest.getNewProvider();
        final AuthenticationHandler auth = AuthenticationHandler.tokenBased(secrets, Duration.ofSeconds(60));
        auth.setApiProvider(apiProvider);
        Assertions.assertThat(auth.execute((a) -> Collections.emptyList())).isEmpty();
        Mockito.verify(zonkyOauth, Mockito.never()).refresh(ArgumentMatchers.any());
        Mockito.verify(zonkyOauth, Mockito.times(1))
                .login(ArgumentMatchers.eq(secrets.getUsername()), ArgumentMatchers.eq(secrets.getPassword()));
        Mockito.verify(z, Mockito.never()).logout();
    }

    @Test
    public void tokenBasedWithExpiredToken() throws IOException, KeyStoreException, JAXBException {
        final OffsetDateTime expired = OffsetDateTime.now().minus(300, ChronoUnit.SECONDS);
        final SecretProvider secrets = AuthenticationHandlerTest.getNewProvider(expired);
        final AuthenticationHandler auth = AuthenticationHandler.tokenBased(secrets, Duration.ofSeconds(60));
        final OAuth zonkyOauth = Mockito.mock(OAuth.class);
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);
        Mockito.when(zonkyOauth.login(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(token);
        final ApiProvider apiProvider = Mockito.spy(new ApiProvider());
        auth.setApiProvider(apiProvider);
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.doReturn(zonkyOauth).when(apiProvider).oauth();
        Mockito.doReturn(z).when(apiProvider).authenticated(Mockito.eq(AuthenticatorTest.TOKEN));
        Assertions.assertThat(auth.execute((a) -> Collections.emptyList())).isEmpty();
        Mockito.verify(zonkyOauth, Mockito.never()).refresh(ArgumentMatchers.any());
        Mockito.verify(zonkyOauth, Mockito.times(1))
                .login(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(z, Mockito.never()).logout();
    }

    @Test
    public void tokenBasedWithExpiringToken() throws JAXBException {
        final OffsetDateTime expiring = OffsetDateTime.now().minus(250, ChronoUnit.SECONDS);
        final SecretProvider secrets = AuthenticationHandlerTest.getNewProvider(expiring);
        final AuthenticationHandler auth = AuthenticationHandler.tokenBased(secrets, Duration.ofSeconds(60));
        final OAuth zonkyOauth = Mockito.mock(OAuth.class);
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);
        Mockito.when(zonkyOauth.refresh(ArgumentMatchers.any())).thenReturn(token);
        final ApiProvider apiProvider = Mockito.spy(new ApiProvider());
        auth.setApiProvider(apiProvider);;
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.doReturn(zonkyOauth).when(apiProvider).oauth();
        Mockito.doReturn(z).when(apiProvider).authenticated(Mockito.eq(AuthenticatorTest.TOKEN));
        Assertions.assertThat(auth.execute((a) -> Collections.emptyList())).isEmpty();
        Mockito.verify(zonkyOauth, Mockito.times(1)).refresh(ArgumentMatchers.any());
        Mockito.verify(zonkyOauth, Mockito.never()).login(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(z, Mockito.never()).logout();
    }

    @Test
    public void tokenBasedWithImmediatelyExpiringToken() throws JAXBException {
        final OffsetDateTime expiring = OffsetDateTime.now().minus(298, ChronoUnit.SECONDS);
        final SecretProvider secrets = AuthenticationHandlerTest.getNewProvider(expiring);
        final AuthenticationHandler auth = AuthenticationHandler.tokenBased(secrets, Duration.ofSeconds(60));
        final OAuth zonkyOauth = Mockito.mock(OAuth.class);
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);
        Mockito.when(zonkyOauth.login(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(token);
        final ApiProvider apiProvider = Mockito.spy(new ApiProvider());
        auth.setApiProvider(apiProvider);
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.doReturn(zonkyOauth).when(apiProvider).oauth();
        Mockito.doReturn(z).when(apiProvider).authenticated(Mockito.eq(AuthenticatorTest.TOKEN));
        Assertions.assertThat(auth.execute((a) -> Collections.emptyList())).isEmpty();
        Mockito.verify(zonkyOauth, Mockito.never()).refresh(ArgumentMatchers.any());
        Mockito.verify(zonkyOauth, Mockito.times(1))
                .login(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(z, Mockito.never()).logout();
    }

    @Test
    public void failWhileStoringToken() throws JAXBException {
        final SecretProvider provider = Mockito.mock(SecretProvider.class);
        Mockito.when(provider.setToken(ArgumentMatchers.any())).thenReturn(false);
        final AuthenticationHandler auth = AuthenticationHandler.passwordBased(provider);
        final ZonkyApiToken t = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);
        Assertions.assertThat(auth.storeToken(t)).isFalse();
    }

}

