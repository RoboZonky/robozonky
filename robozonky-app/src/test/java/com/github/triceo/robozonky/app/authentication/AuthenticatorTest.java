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

import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.ControlApi;
import com.github.triceo.robozonky.api.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.common.remote.Apis;
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

    static final ZonkyApiToken TOKEN =
            new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);

    private static ResteasyClientBuilder mockResteasy() {
        final ZonkyOAuthApi mock = Mockito.mock(ZonkyOAuthApi.class);
        Mockito.when(mock.login(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenReturn(AuthenticatorTest.TOKEN);
        Mockito.when(mock.refresh(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new ZonkyApiToken(String.valueOf(AuthenticatorTest.TOKEN.getRefreshToken()), UUID
                        .randomUUID().toString(), 300));
        final ResteasyWebTarget target = Mockito.mock(ResteasyWebTarget.class);
        Mockito.when(target.proxy(ZonkyOAuthApi.class)).thenReturn(mock);
        Mockito.when(target.proxy(ControlApi.class)).thenReturn(Mockito.mock(ControlApi.class));
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
        final Apis provider = Mockito.mock(Apis.class);
        Mockito.when(provider.oauth()).thenReturn(new Apis.Wrapper<>(apiMock));
        final Function<Apis, ZonkyApiToken> a = Authenticator.withCredentials(AuthenticatorTest.DUMMY_USER,
                AuthenticatorTest.DUMMY_PWD.toCharArray());
        final ZonkyApiToken auth = a.apply(provider);
        Assertions.assertThat(auth).isNotNull();
        Mockito.verify(apiMock, Mockito.times(1))
                .login(ArgumentMatchers.eq(AuthenticatorTest.DUMMY_USER),
                        ArgumentMatchers.eq(AuthenticatorTest.DUMMY_PWD), ArgumentMatchers.any(),
                        ArgumentMatchers.any());
    }

    @Test
    public void tokenBasedAuthentication() {
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy();
        final ZonkyOAuthApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyOAuthApi.class);
        final Apis provider = Mockito.mock(Apis.class);
        Mockito.when(provider.oauth()).thenReturn(new Apis.Wrapper<>(apiMock));
        final Function<Apis, ZonkyApiToken> a =
                Authenticator.withAccessToken(AuthenticatorTest.DUMMY_USER, AuthenticatorTest.TOKEN, Duration.ZERO);
        final ZonkyApiToken auth = a.apply(provider);
        Assertions.assertThat(auth).isEqualTo(AuthenticatorTest.TOKEN);
        Mockito.verify(apiMock, Mockito.never()).login(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(apiMock, Mockito.never()).refresh(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any());
    }

    @Test
    public void tokenBasedAuthenticationWithRefresh() {
        final ResteasyClientBuilder mock = AuthenticatorTest.mockResteasy();
        final ZonkyOAuthApi apiMock = mock.build().target(AuthenticatorTest.DUMMY_URL).proxy(ZonkyOAuthApi.class);
        final Apis provider = Mockito.mock(Apis.class);
        Mockito.when(provider.oauth()).thenReturn(new Apis.Wrapper<>(apiMock));
        final ZonkyApiToken result =
                Authenticator.withAccessToken(AuthenticatorTest.DUMMY_USER, AuthenticatorTest.TOKEN,
                        Duration.ofSeconds(AuthenticatorTest.TOKEN.getExpiresIn())).apply(provider);
        Mockito.verify(apiMock, Mockito.never()).login(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(apiMock, Mockito.times(1)).refresh(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
        Assertions.assertThat(result).isNotNull().isNotEqualTo(AuthenticatorTest.TOKEN);
    }

}
