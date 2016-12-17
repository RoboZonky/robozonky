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

package com.github.triceo.robozonky;

import java.util.UUID;

import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.api.remote.ZotifyApi;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import org.assertj.core.api.Assertions;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
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

    private static final ZonkyApiToken TOKEN = new ZonkyApiToken(UUID.randomUUID().toString(),
            UUID.randomUUID().toString(), 300);

    private static ResteasyClientBuilder mockResteasy() {
        final ZonkyOAuthApi mock = Mockito.mock(ZonkyOAuthApi.class);
        Mockito.when(mock.login(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenReturn(AuthenticatorTest.TOKEN);
        Mockito.when(mock.refresh(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new ZonkyApiToken(String.valueOf(AuthenticatorTest.TOKEN.getRefreshToken()), UUID
                        .randomUUID().toString(), 300));
        final ResteasyWebTarget target = Mockito.mock(ResteasyWebTarget.class);
        Mockito.when(target.proxy(ZonkyOAuthApi.class)).thenReturn(mock);
        Mockito.when(target.proxy(ZonkyApi.class)).thenReturn(Mockito.mock(ZonkyApi.class));
        Mockito.when(target.proxy(ZotifyApi.class)).thenReturn(Mockito.mock(ZotifyApi.class));
        final ResteasyClient client =
                Mockito.mock(ResteasyClient.class, new AuthenticatorTest.AnswerWithSelf(ResteasyClient.class));
        Mockito.when(client.target(ArgumentMatchers.anyString())).thenReturn(target);
        final ResteasyClientBuilder builder = Mockito.mock(ResteasyClientBuilder.class);
        Mockito.when(builder.build()).thenReturn(client);
        return builder;
    }

    @Test
    public void credentialBasedAuthentication() {
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy();
        final ZonkyOAuthApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyOAuthApi.class);
        final ApiProvider provider = new ApiProvider(mock);
        Authenticator.withCredentials(AuthenticatorTest.DUMMY_USER, AuthenticatorTest.DUMMY_PWD.toCharArray())
                .authenticate(provider);
        Mockito.verify(apiMock, Mockito.times(1))
                .login(Mockito.eq(AuthenticatorTest.DUMMY_USER), Mockito.eq(AuthenticatorTest.DUMMY_PWD),
                        Mockito.any(), Mockito.any());
        final ResteasyWebTarget target = mock.build().target(AuthenticatorTest.DUMMY_URL);
        Mockito.verify(target, Mockito.times(1)).proxy(ZonkyApi.class);
    }

    @Test
    public void credentialBasedAuthenticationDry() {
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy();
        final ZonkyOAuthApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyOAuthApi.class);
        final ApiProvider provider = new ApiProvider(mock);
        Authenticator.withCredentials(AuthenticatorTest.DUMMY_USER, AuthenticatorTest.DUMMY_PWD.toCharArray())
                .authenticate(provider);
        Mockito.verify(apiMock, Mockito.times(1))
                .login(Mockito.eq(AuthenticatorTest.DUMMY_USER), Mockito.eq(AuthenticatorTest.DUMMY_PWD),
                        Mockito.any(), Mockito.any());
        final ResteasyWebTarget target = mock.build().target(AuthenticatorTest.DUMMY_URL);
        Mockito.verify(target, Mockito.atLeastOnce()).proxy(ZonkyApi.class);
    }

    @Test
    public void tokenBasedAuthentication() {
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy();
        final ZonkyOAuthApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyOAuthApi.class);
        final ApiProvider provider = new ApiProvider(mock);
        final Authentication result =
                Authenticator.withAccessToken(AuthenticatorTest.DUMMY_USER, AuthenticatorTest.TOKEN)
                        .authenticate(provider);
        Mockito.verify(apiMock, Mockito.never()).login(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(apiMock, Mockito.never()).refresh(Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertThat(result.getZonkyApiToken()).isEqualTo(AuthenticatorTest.TOKEN);
    }

    @Test
    public void tokenBasedAuthenticationWithRefresh() {
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy();
        final ZonkyOAuthApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyOAuthApi.class);
        final ApiProvider provider = new ApiProvider(mock);
        final Authentication result =
                Authenticator.withAccessTokenAndRefresh(AuthenticatorTest.DUMMY_USER, AuthenticatorTest.TOKEN)
                        .authenticate(provider);
        Mockito.verify(apiMock, Mockito.never()).login(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(apiMock, Mockito.times(1)).refresh(Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertThat(result.getZonkyApiToken()).isNotEqualTo(AuthenticatorTest.TOKEN);
    }

}
