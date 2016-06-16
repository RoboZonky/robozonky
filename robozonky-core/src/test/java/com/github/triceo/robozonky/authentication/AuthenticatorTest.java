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

package com.github.triceo.robozonky.authentication;

import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import org.assertj.core.api.Assertions;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Test;
import org.mockito.Mockito;

public class AuthenticatorTest {

    private static final String DUMMY_URL = "http://localhost";
    private static final String DUMMY_USER = "a";
    private static final String DUMMY_PWD = "b";

    public static ResteasyClientBuilder mockResteasy(final String url, final ZonkyApiToken token) {
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.login(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(token);
        Mockito.when(api.refresh(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mockito.mock(ZonkyApiToken.class));
        final ResteasyWebTarget target = Mockito.mock(ResteasyWebTarget.class);
        Mockito.when(target.proxy(ZonkyApi.class)).thenReturn(api);
        final ResteasyClient client = Mockito.mock(ResteasyClient.class);
        Mockito.when(client.target(url)).thenReturn(target);
        final ResteasyClientBuilder builder = Mockito.mock(ResteasyClientBuilder.class);
        Mockito.when(builder.build()).thenReturn(client);
        return builder;
    }

    public static ResteasyClientBuilder mockResteasy(final String url) {
        return AuthenticatorTest.mockResteasy(url, Mockito.mock(ZonkyApiToken.class));
    }

    @Test
    public void credentialBasedAuthentication() {
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy(AuthenticatorTest.DUMMY_URL);
        final ZonkyApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyApi.class);
        Authenticator.withCredentials(AuthenticatorTest.DUMMY_USER, AuthenticatorTest.DUMMY_PWD)
                .authenticate(AuthenticatorTest.DUMMY_URL, AuthenticatorTest.DUMMY_URL, "UNDEFINED", mock);
        Mockito.verify(apiMock, Mockito.times(1))
                .login(Mockito.eq(AuthenticatorTest.DUMMY_USER), Mockito.eq(AuthenticatorTest.DUMMY_PWD),
                        Mockito.any(), Mockito.any());
    }

    @Test
    public void tokenBasedAuthentication() {
        final ZonkyApiToken mockToken = Mockito.mock(ZonkyApiToken.class);
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy(AuthenticatorTest.DUMMY_URL, mockToken);
        final ZonkyApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyApi.class);
        final Authentication result = Authenticator.withAccessToken(AuthenticatorTest.DUMMY_USER, mockToken)
                .authenticate(AuthenticatorTest.DUMMY_URL, AuthenticatorTest.DUMMY_URL, "UNDEFINED", mock);
        Mockito.verify(apiMock, Mockito.never())
                .login(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(apiMock, Mockito.never()).refresh(Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertThat(result.getZonkyApiToken()).isEqualTo(mockToken);
    }

    @Test
    public void tokenBasedAuthenticationWithRefresh() {
        final ZonkyApiToken mockToken = Mockito.mock(ZonkyApiToken.class);
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy(AuthenticatorTest.DUMMY_URL, mockToken);
        final ZonkyApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyApi.class);
        final Authentication result = Authenticator.withAccessTokenAndRefresh(AuthenticatorTest.DUMMY_USER, mockToken)
                .authenticate(AuthenticatorTest.DUMMY_URL, AuthenticatorTest.DUMMY_URL, "UNDEFINED", mock);
        Mockito.verify(apiMock, Mockito.never())
                .login(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(apiMock, Mockito.times(1)).refresh(Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertThat(result.getZonkyApiToken()).isNotEqualTo(mockToken);
    }

}
