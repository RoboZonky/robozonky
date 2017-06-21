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

import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.github.triceo.robozonky.common.remote.OAuth;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AuthenticatorTest {

    private static final String DUMMY_USER = "a";
    private static final String DUMMY_PWD = "b";

    static final ZonkyApiToken TOKEN =
            new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);

    @Test
    public void credentialBasedAuthentication() {
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        final OAuth oauth = Mockito.mock(OAuth.class);
        Mockito.when(provider.oauth()).thenReturn(oauth);
        final Function<ApiProvider, ZonkyApiToken> a = Authenticator.withCredentials(AuthenticatorTest.DUMMY_USER,
                AuthenticatorTest.DUMMY_PWD.toCharArray());
        a.apply(provider);
        Mockito.verify(oauth, Mockito.times(1))
                .login(ArgumentMatchers.eq(AuthenticatorTest.DUMMY_USER),
                        ArgumentMatchers.eq(AuthenticatorTest.DUMMY_PWD.toCharArray()));
    }

    @Test
    public void tokenBasedAuthentication() {
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        final OAuth oauth = Mockito.mock(OAuth.class);
        Mockito.when(provider.oauth()).thenReturn(oauth);
        final Function<ApiProvider, ZonkyApiToken> a =
                Authenticator.withAccessToken(AuthenticatorTest.DUMMY_USER, AuthenticatorTest.TOKEN, Duration.ZERO);
        final ZonkyApiToken auth = a.apply(provider);
        Assertions.assertThat(auth).isEqualTo(AuthenticatorTest.TOKEN);
        Mockito.verify(oauth, Mockito.never()).login(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(oauth, Mockito.never()).refresh(ArgumentMatchers.any());
    }

    @Test
    public void tokenBasedAuthenticationWithRefresh() {
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        final OAuth oauth = Mockito.mock(OAuth.class);
        Mockito.when(provider.oauth()).thenReturn(oauth);
        Authenticator.withAccessToken(AuthenticatorTest.DUMMY_USER, AuthenticatorTest.TOKEN,
                Duration.ofSeconds(AuthenticatorTest.TOKEN.getExpiresIn())).apply(provider);
        Mockito.verify(oauth, Mockito.never()).login(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(oauth, Mockito.times(1)).refresh(ArgumentMatchers.any());
    }

}
