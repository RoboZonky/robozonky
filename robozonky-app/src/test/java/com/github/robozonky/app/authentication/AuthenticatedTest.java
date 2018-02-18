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
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class AuthenticatedTest extends AbstractZonkyLeveragingTest {

    private static ApiProvider mockApiProvider(final OAuth oauth, final Zonky z) {
        final ApiProvider api = mock(ApiProvider.class);
        when(api.oauth(any(Function.class))).then(i -> {
            final Function f = i.getArgument(0);
            return f.apply(oauth);
        });
        when(api.authenticated(any(ZonkyApiToken.class), any(Function.class)))
                .then(i -> {
                    final Function f = i.getArgument(1);
                    return f.apply(z);
                });
        return api;
    }

    @Test
    void restrictions() {
        final Zonky z = mock(Zonky.class);
        when(z.getRestrictions()).thenAnswer(invocation -> mock(Restrictions.class));
        final AbstractAuthenticated a = new AbstractAuthenticated() {
            @Override
            public <T> T call(final Function<Zonky, T> operation) {
                return operation.apply(z);
            }

            @Override
            public SecretProvider getSecretProvider() {
                return null;
            }
        };
        final Restrictions r = a.getRestrictions(Instant.now().minus(Duration.ofMinutes(10))); // stale
        assertThat(r).isNotNull();
        final Restrictions r2 = a.getRestrictions(); // should refresh
        assertThat(r2).isNotNull().isNotEqualTo(r);
    }

    @Test
    void run() {
        final Zonky z = mock(Zonky.class);
        final AbstractAuthenticated a = new AbstractAuthenticated() {
            @Override
            public <T> T call(final Function<Zonky, T> operation) {
                return operation.apply(z);
            }

            @Override
            public SecretProvider getSecretProvider() {
                return null;
            }
        };
        final Consumer<Zonky> runnable = mock(Consumer.class);
        a.run(runnable);
        verify(runnable).accept(eq(z));
    }

    @Test
    void defaultMethod() {
        final Zonky z = mock(Zonky.class);
        final Authenticated a = mockAuthentication(z);
        final Consumer<Zonky> c = zonky -> z.logout();
        a.run(c);
        verify(z).logout();
    }

    @Test
    void passwordProper() {
        // prepare SUT
        final SecretProvider sp = SecretProvider.fallback(UUID.randomUUID().toString(), new char[0]);
        final String username = sp.getUsername();
        final char[] password = sp.getPassword();
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);
        final OAuth oauth = mock(OAuth.class);
        when(oauth.login(eq(username), eq(password))).thenReturn(token);
        final Zonky z = mock(Zonky.class);
        final ApiProvider api = mockApiProvider(oauth, z);
        final Authenticated a = Authenticated.passwordBased(api, sp);
        // call SUT
        final Function<Zonky, Collection<RawInvestment>> f = mock(Function.class);
        final Collection<RawInvestment> expectedResult = Collections.emptyList();
        when(f.apply(eq(z))).thenReturn(expectedResult);
        final Collection<RawInvestment> result = a.call(f);
        assertThat(result).isSameAs(expectedResult);
        verify(oauth).login(eq(username), eq(password));
        verify(oauth, never()).refresh(any());
        verify(z).logout();
    }

    @Test
    void passwordLogsOutEvenWhenFailing() {
        // prepare SUT
        final SecretProvider sp = SecretProvider.fallback(UUID.randomUUID().toString(), new char[0]);
        final String username = sp.getUsername();
        final char[] password = sp.getPassword();
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);
        final OAuth oauth = mock(OAuth.class);
        when(oauth.login(eq(username), eq(password))).thenReturn(token);
        final Zonky z = mock(Zonky.class);
        final ApiProvider api = mockApiProvider(oauth, z);
        final Authenticated a = Authenticated.passwordBased(api, sp);
        // call SUT
        final Function<Zonky, Collection<RawInvestment>> f = mock(Function.class);
        when(f.apply(eq(z))).thenThrow(new IllegalStateException());
        assertThatThrownBy(() -> a.call(f)).isInstanceOf(IllegalStateException.class);
        verify(z).logout();
    }

    @Test
    void tokenProper() {
        // prepare SUT
        final SecretProvider sp = SecretProvider.fallback(UUID.randomUUID().toString(), new char[0]);
        final String username = sp.getUsername();
        final char[] password = sp.getPassword();
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 299);
        final OAuth oauth = mock(OAuth.class);
        when(oauth.login(eq(username), eq(password))).thenReturn(token);
        final Zonky z = mock(Zonky.class);
        final ApiProvider api = mockApiProvider(oauth, z);
        final TokenBasedAccess a = (TokenBasedAccess) Authenticated.tokenBased(api, sp, Duration.ofSeconds(60));
        // call SUT
        final Function<Zonky, Collection<RawInvestment>> f = mock(Function.class);
        final Collection<RawInvestment> expectedResult = Collections.emptyList();
        when(f.apply(eq(z))).thenReturn(expectedResult);
        final Collection<RawInvestment> result = a.call(f);
        assertSoftly(softly -> {
            softly.assertThat(result).isSameAs(expectedResult);
            softly.assertThat(a.getSecretProvider()).isSameAs(sp);
        });
        verify(oauth).login(eq(username), eq(password));
        verify(oauth, never()).refresh(any());
        verify(z, never()).logout();
    }

}
