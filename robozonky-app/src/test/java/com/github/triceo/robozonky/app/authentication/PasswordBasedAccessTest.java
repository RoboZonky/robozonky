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

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.github.triceo.robozonky.common.remote.OAuth;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class PasswordBasedAccessTest {

    @Test
    public void proper() {
        // prepare SUT
        final SecretProvider sp = SecretProvider.fallback(UUID.randomUUID().toString(), new char[0]);
        final String username = sp.getUsername();
        final char[] password = sp.getPassword();
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);
        final OAuth oauth = Mockito.mock(OAuth.class);
        Mockito.when(oauth.login(ArgumentMatchers.eq(username), ArgumentMatchers.eq(password))).thenReturn(token);
        final ApiProvider api = Mockito.mock(ApiProvider.class);
        Mockito.when(api.oauth()).thenReturn(oauth);
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.when(api.authenticated(ArgumentMatchers.eq(token))).thenReturn(z);
        final Authenticated a = Authenticated.passwordBased(api, sp);
        // execute SUT
        final Function<Zonky, Collection<Investment>> f = Mockito.mock(Function.class);
        final Collection<Investment> expectedResult = Collections.emptyList();
        Mockito.when(f.apply(ArgumentMatchers.eq(z))).thenReturn(expectedResult);
        final Collection<Investment> result = a.execute(f);
        Assertions.assertThat(result).isSameAs(expectedResult);
        Mockito.verify(oauth).login(ArgumentMatchers.eq(username), ArgumentMatchers.eq(password));
        Mockito.verify(oauth, Mockito.never()).refresh(ArgumentMatchers.any());
        Mockito.verify(z).logout();
    }

    @Test
    public void logsOutEvenWhenFailing() {
        // prepare SUT
        final SecretProvider sp = SecretProvider.fallback(UUID.randomUUID().toString(), new char[0]);
        final String username = sp.getUsername();
        final char[] password = sp.getPassword();
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);
        final OAuth oauth = Mockito.mock(OAuth.class);
        Mockito.when(oauth.login(ArgumentMatchers.eq(username), ArgumentMatchers.eq(password))).thenReturn(token);
        final ApiProvider api = Mockito.mock(ApiProvider.class);
        Mockito.when(api.oauth()).thenReturn(oauth);
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.when(api.authenticated(ArgumentMatchers.eq(token))).thenReturn(z);
        final Authenticated a = Authenticated.passwordBased(api, sp);
        // execute SUT
        final Function<Zonky, Collection<Investment>> f = Mockito.mock(Function.class);
        Mockito.when(f.apply(ArgumentMatchers.eq(z))).thenThrow(new IllegalStateException());
        Assertions.assertThatThrownBy(() -> a.execute(f)).isInstanceOf(IllegalStateException.class);
        Mockito.verify(z).logout();
    }
}
