/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.tenant;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.api.remote.enums.OAuthScope;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.util.DateUtil;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ZonkyApiTokenSupplierTest extends AbstractZonkyLeveragingTest {

    private static final SecretProvider SECRETS = SecretProvider.inMemory("someone", "password".toCharArray());

    private static ZonkyApiToken getTokenExpiringIn(final Duration duration) {
        return new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                 OffsetDateTime.ofInstant(DateUtil.now().minus(Duration.ofMinutes(5)).plus(duration),
                                                          Defaults.ZONE_ID));
    }

    private static ApiProvider mockApi(final OAuth oAuth, final Zonky zonky) {
        final ApiProvider api = mock(ApiProvider.class);
        when(api.oauth(any())).thenAnswer(invocation -> {
            final Function<OAuth, Object> f = invocation.getArgument(0);
            return f.apply(oAuth);
        });
        when(api.call(any(), any())).then(i -> {
            final Supplier<ZonkyApiToken> s = i.getArgument(1);
            s.get();
            final Function<Zonky, ?> f = i.getArgument(0);
            return f.apply(zonky);
        });
        doAnswer((Answer<Void>) invocation -> {
            final Consumer<Zonky> f = invocation.getArgument(0);
            f.accept(zonky);
            return null;
        }).when(api).run(any(), any());
        return api;
    }

    @Test
    void refreshes() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        final ZonkyApiToken token = getTokenExpiringIn(Duration.ofMinutes(5));
        when(oAuth.login(eq(OAuthScope.SCOPE_APP_WEB), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> token);
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, SECRETS);
        assertThat(t.get()).isEqualTo(token);
        skipAheadBy(Duration.ofSeconds(4 * 60 + 56)); // get over the refresh period
        final ZonkyApiToken secondToken = getTokenExpiringIn(Duration.ofMinutes(5));
        when(oAuth.refresh(any())).thenReturn(secondToken);
        assertThat(t.get()).isEqualTo(secondToken);
    }

    @Test
    void newLoginWhenTokenExpiredWithoutRefresh() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        final ZonkyApiToken token = getTokenExpiringIn(Duration.ofMinutes(5));
        when(oAuth.login(eq(OAuthScope.SCOPE_APP_WEB), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> token);
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, SECRETS);
        assertThat(t.get()).isEqualTo(token);
        skipAheadBy(Duration.ofMinutes(6)); // get over the expiration period
        final ZonkyApiToken secondToken = getTokenExpiringIn(Duration.ofMinutes(5));
        when(oAuth.login(eq(OAuthScope.SCOPE_APP_WEB), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> secondToken);
        assertThat(t.get()).isEqualTo(secondToken);
    }

    @Test
    void reloginsWhenAlreadyExpired() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        final ZonkyApiToken token = getTokenExpiringIn(Duration.ofMinutes(5));
        when(oAuth.login(eq(OAuthScope.SCOPE_APP_WEB), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> token);
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, SECRETS);
        assertThat(t.get()).isEqualTo(token);
        skipAheadBy(Duration.ofMinutes(6)); // get over the expiration period
        final ZonkyApiToken secondToken = getTokenExpiringIn(Duration.ofMinutes(5));
        when(oAuth.login(eq(OAuthScope.SCOPE_APP_WEB), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> secondToken);
        assertThat(t.get()).isEqualTo(secondToken);
    }

    @Test
    void failsOnLogin() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        doThrow(IllegalStateException.class).when(oAuth).login(any(), any(), any());
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, SECRETS);
        assertThatThrownBy(t::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failsOnRefresh() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        when(oAuth.login(eq(OAuthScope.SCOPE_APP_WEB), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> getTokenExpiringIn(Duration.ofMinutes(5)));
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, SECRETS);
        final ZonkyApiToken token = t.get();
        assertThat(token).isNotNull();
        skipAheadBy(Duration.ofSeconds(4 * 60 + 55)); // get over the refresh period, but not over expiration
        doThrow(IllegalStateException.class).when(oAuth).refresh(any());
        assertThat(t.get())
                .isNotNull()
                .isNotSameAs(token);
        verify(oAuth).refresh(any()); // make sure refresh was rejected before login was called
    }

    @Test
    void closingNeverLoaded() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        final ZonkyApiToken token = getTokenExpiringIn(Duration.ofSeconds(5));
        when(oAuth.login(eq(OAuthScope.SCOPE_APP_WEB), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> token);
        when(oAuth.refresh(any())).thenReturn(token);
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, SECRETS);
        t.close();
        verify(oAuth, never()).login(any(), any(), any());
        verify(zonky, never()).logout();
        assertThatThrownBy(t::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void closingLoaded() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        final ZonkyApiToken token = getTokenExpiringIn(Duration.ofSeconds(5));
        when(oAuth.login(eq(OAuthScope.SCOPE_APP_WEB), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> token);
        when(oAuth.refresh(any())).thenReturn(token);
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, SECRETS);
        t.get();
        verify(oAuth).login(any(), any(), any());
        assertThat(t.isClosed()).isFalse();
        t.close();
        verify(zonky, only()).logout();
        assertThat(t.isClosed()).isTrue();
        assertThatThrownBy(t::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void notClosingWhenExpired() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        final ZonkyApiToken token = getTokenExpiringIn(Duration.ZERO);
        when(oAuth.login(eq(OAuthScope.SCOPE_APP_WEB), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> token);
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, SECRETS);
        t.close();
        verify(zonky, never()).logout();
    }
}
