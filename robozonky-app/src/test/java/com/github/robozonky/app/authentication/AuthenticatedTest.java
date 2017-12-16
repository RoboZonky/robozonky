/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.authentication;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.ws.rs.NotAuthorizedException;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AuthenticatedTest extends AbstractZonkyLeveragingTest {

    private static ApiProvider mockApiProvider(final OAuth oauth, final Zonky z) {
        final ApiProvider api = Mockito.mock(ApiProvider.class);
        Mockito.when(api.oauth(ArgumentMatchers.any(Function.class))).then(i -> {
            final Function f = i.getArgument(0);
            return f.apply(oauth);
        });
        Mockito.when(api.authenticated(ArgumentMatchers.any(ZonkyApiToken.class), ArgumentMatchers.any(Function.class)))
                .then(i -> {
                    final Function f = i.getArgument(1);
                    return f.apply(z);
                });
        return api;
    }

    @Test
    public void defaultMethod() {
        final Zonky z = Mockito.mock(Zonky.class);
        final Authenticated a = mockAuthentication(z);
        final Consumer<Zonky> c = zonky -> z.logout();
        a.run(c);
        Mockito.verify(z).logout();
    }

    @Test
    public void passwordProper() {
        // prepare SUT
        final SecretProvider sp = SecretProvider.fallback(UUID.randomUUID().toString(), new char[0]);
        final String username = sp.getUsername();
        final char[] password = sp.getPassword();
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);
        final OAuth oauth = Mockito.mock(OAuth.class);
        Mockito.when(oauth.login(ArgumentMatchers.eq(username), ArgumentMatchers.eq(password))).thenReturn(token);
        final Zonky z = Mockito.mock(Zonky.class);
        final ApiProvider api = mockApiProvider(oauth, z);
        final Authenticated a = Authenticated.passwordBased(api, sp);
        // call SUT
        final Function<Zonky, Collection<Investment>> f = Mockito.mock(Function.class);
        final Collection<Investment> expectedResult = Collections.emptyList();
        Mockito.when(f.apply(ArgumentMatchers.eq(z))).thenReturn(expectedResult);
        final Collection<Investment> result = a.call(f);
        Assertions.assertThat(result).isSameAs(expectedResult);
        Mockito.verify(oauth).login(ArgumentMatchers.eq(username), ArgumentMatchers.eq(password));
        Mockito.verify(oauth, Mockito.never()).refresh(ArgumentMatchers.any());
        Mockito.verify(z).logout();
    }

    @Test
    public void passwordLogsOutEvenWhenFailing() {
        // prepare SUT
        final SecretProvider sp = SecretProvider.fallback(UUID.randomUUID().toString(), new char[0]);
        final String username = sp.getUsername();
        final char[] password = sp.getPassword();
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);
        final OAuth oauth = Mockito.mock(OAuth.class);
        Mockito.when(oauth.login(ArgumentMatchers.eq(username), ArgumentMatchers.eq(password))).thenReturn(token);
        final Zonky z = Mockito.mock(Zonky.class);
        final ApiProvider api = mockApiProvider(oauth, z);
        final Authenticated a = Authenticated.passwordBased(api, sp);
        // call SUT
        final Function<Zonky, Collection<Investment>> f = Mockito.mock(Function.class);
        Mockito.when(f.apply(ArgumentMatchers.eq(z))).thenThrow(new IllegalStateException());
        Assertions.assertThatThrownBy(() -> a.call(f)).isInstanceOf(IllegalStateException.class);
        Mockito.verify(z).logout();
    }

    @Test
    public void tokenProper() {
        // prepare SUT
        final SecretProvider sp = SecretProvider.fallback(UUID.randomUUID().toString(), new char[0]);
        final String username = sp.getUsername();
        final char[] password = sp.getPassword();
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);
        final OAuth oauth = Mockito.mock(OAuth.class);
        Mockito.when(oauth.login(ArgumentMatchers.eq(username), ArgumentMatchers.eq(password))).thenReturn(token);
        final Zonky z = Mockito.mock(Zonky.class);
        final ApiProvider api = mockApiProvider(oauth, z);
        final TokenBasedAccess a = (TokenBasedAccess) Authenticated.tokenBased(api, sp, Duration.ofSeconds(60));
        // call SUT
        final Function<Zonky, Collection<Investment>> f = Mockito.mock(Function.class);
        final Collection<Investment> expectedResult = Collections.emptyList();
        Mockito.when(f.apply(ArgumentMatchers.eq(z))).thenReturn(expectedResult);
        final Collection<Investment> result = a.call(f);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isSameAs(expectedResult);
            softly.assertThat(a.getSecretProvider()).isSameAs(sp);
            softly.assertThat(a.getRefreshableToken().isPaused()).isFalse();
        });
        Mockito.verify(oauth).login(ArgumentMatchers.eq(username), ArgumentMatchers.eq(password));
        Mockito.verify(oauth, Mockito.never()).refresh(ArgumentMatchers.any());
        Mockito.verify(z, Mockito.never()).logout();
    }

    @Test
    public void tokenReattempt() {
        // prepare SUT
        final SecretProvider sp = SecretProvider.fallback(UUID.randomUUID().toString(), new char[0]);
        final String username = sp.getUsername();
        final char[] password = sp.getPassword();
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 1);
        final OAuth oauth = Mockito.mock(OAuth.class);
        Mockito.when(oauth.login(ArgumentMatchers.eq(username), ArgumentMatchers.eq(password))).thenReturn(token);
        final ZonkyApiToken newToken = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                                         299);
        Mockito.when(oauth.refresh(ArgumentMatchers.eq(token))).thenReturn(newToken);
        final Zonky z = Mockito.mock(Zonky.class);
        final ApiProvider api = mockApiProvider(oauth, z);
        final TemporalAmount never = Duration.ofDays(1000); // let's not auto-refresh during the test
        final TokenBasedAccess a = (TokenBasedAccess) Authenticated.tokenBased(api, sp, never);
        // call SUT
        final Consumer<Zonky> f = Mockito.mock(Consumer.class);
        Mockito.doThrow(NotAuthorizedException.class).when(f).accept(z);
        Assertions.assertThatThrownBy(() -> a.run(f)).isInstanceOf(IllegalStateException.class);
        Mockito.verify(oauth).refresh(ArgumentMatchers.any());
        Mockito.verify(f, Mockito.times(3)).accept(z); // three attempts to execute
    }
}
