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

import com.github.triceo.robozonky.ApiProvider;
import com.github.triceo.robozonky.remote.InvestingZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import com.github.triceo.robozonky.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.remote.ZotifyApi;
import org.assertj.core.api.Assertions;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.defaultanswers.ReturnsEmptyValues;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class AuthenticatorTest {

    private static final String DUMMY_URL = "http://localhost";
    private static final String DUMMY_USER = "a";
    private static final String DUMMY_PWD = "b";

    private static class AnswerWithSelf implements Answer<Object> {
        private final Answer<Object> delegate = new ReturnsEmptyValues();
        private final Class<?> clazz;

        public AnswerWithSelf(final Class<?> clazz) {
            this.clazz = clazz;
        }

        public Object answer(final InvocationOnMock invocation) throws Throwable {
            final Class<?> returnType = invocation.getMethod().getReturnType();
            if (returnType == clazz) {
                return invocation.getMock();
            } else {
                return delegate.answer(invocation);
            }
        }
    }

    private static ResteasyClientBuilder mockResteasy(final ZonkyApiToken token) {
        final ZonkyOAuthApi mock = Mockito.mock(ZonkyOAuthApi.class);
        Mockito.when(mock.login(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(token);
        Mockito.when(mock.refresh(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mockito.mock(ZonkyApiToken.class));
        final ResteasyWebTarget target = Mockito.mock(ResteasyWebTarget.class);
        Mockito.when(target.proxy(ZonkyOAuthApi.class)).thenReturn(mock);
        Mockito.when(target.proxy(ZonkyApi.class)).thenReturn(Mockito.mock(ZonkyApi.class));
        Mockito.when(target.proxy(InvestingZonkyApi.class)).thenReturn(Mockito.mock(InvestingZonkyApi.class));
        Mockito.when(target.proxy(ZotifyApi.class)).thenReturn(Mockito.mock(ZotifyApi.class));
        final ResteasyClient client =
                Mockito.mock(ResteasyClient.class, new AuthenticatorTest.AnswerWithSelf(ResteasyClient.class));
        Mockito.when(client.target(Mockito.anyString())).thenReturn(target);
        final ResteasyClientBuilder builder = Mockito.mock(ResteasyClientBuilder.class);
        Mockito.when(builder.build()).thenReturn(client);
        return builder;
    }

    public static ResteasyClientBuilder mockResteasy() {
        return AuthenticatorTest.mockResteasy(Mockito.mock(ZonkyApiToken.class));
    }

    @Test
    public void credentialBasedAuthentication() {
        final boolean dry = false;
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy();
        final ZonkyOAuthApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyOAuthApi.class);
        final ApiProvider provider = new ApiProvider(mock);
        Authenticator.withCredentials(AuthenticatorTest.DUMMY_USER, AuthenticatorTest.DUMMY_PWD, dry)
                .authenticate(provider);
        Mockito.verify(apiMock, Mockito.times(1))
                .login(Mockito.eq(AuthenticatorTest.DUMMY_USER), Mockito.eq(AuthenticatorTest.DUMMY_PWD),
                        Mockito.any(), Mockito.any());
        final ResteasyWebTarget target = mock.build().target(AuthenticatorTest.DUMMY_URL);
        Mockito.verify(target, Mockito.times(1)).proxy(InvestingZonkyApi.class);
    }

    @Test
    public void credentialBasedAuthenticationDry() {
        final boolean dry = true;
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy();
        final ZonkyOAuthApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyOAuthApi.class);
        final ApiProvider provider = new ApiProvider(mock);
        Authenticator.withCredentials(AuthenticatorTest.DUMMY_USER, AuthenticatorTest.DUMMY_PWD, dry)
                .authenticate(provider);
        Mockito.verify(apiMock, Mockito.times(1))
                .login(Mockito.eq(AuthenticatorTest.DUMMY_USER), Mockito.eq(AuthenticatorTest.DUMMY_PWD),
                        Mockito.any(), Mockito.any());
        final ResteasyWebTarget target = mock.build().target(AuthenticatorTest.DUMMY_URL);
        Mockito.verify(target, Mockito.never()).proxy(InvestingZonkyApi.class);
        Mockito.verify(target, Mockito.atLeastOnce()).proxy(ZonkyApi.class);
    }

    @Test
    public void tokenBasedAuthentication() {
        final boolean dry = false;
        final ZonkyApiToken mockToken = Mockito.mock(ZonkyApiToken.class);
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy(mockToken);
        final ZonkyOAuthApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyOAuthApi.class);
        final ApiProvider provider = new ApiProvider(mock);
        final Authentication result = Authenticator.withAccessToken(AuthenticatorTest.DUMMY_USER, mockToken, dry)
                .authenticate(provider);
        Mockito.verify(apiMock, Mockito.never()).login(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(apiMock, Mockito.never()).refresh(Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertThat(result.getZonkyApiToken()).isEqualTo(mockToken);
    }

    @Test
    public void tokenBasedAuthenticationWithRefresh() {
        final boolean dry = false;
        final ZonkyApiToken mockToken = Mockito.mock(ZonkyApiToken.class);
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy(mockToken);
        final ZonkyOAuthApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyOAuthApi.class);
        final ApiProvider provider = new ApiProvider(mock);
        final Authentication result = Authenticator
                .withAccessTokenAndRefresh(AuthenticatorTest.DUMMY_USER, mockToken, dry).authenticate(provider);
        Mockito.verify(apiMock, Mockito.never()).login(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(apiMock, Mockito.times(1)).refresh(Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertThat(result.getZonkyApiToken()).isNotEqualTo(mockToken);
    }

}
