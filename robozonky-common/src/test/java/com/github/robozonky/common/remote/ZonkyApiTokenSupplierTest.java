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

package com.github.robozonky.common.remote;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.BadRequestException;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZonkyApiTokenSupplierTest {

    private static final SecretProvider SECRETS = SecretProvider.inMemory("someone", "password".toCharArray());

    private static ZonkyApiToken getStaleToken() {
        return new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                 OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
    }

    private static ZonkyApiToken getTokenExpiringIn(final Duration duration) {
        return new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                 OffsetDateTime.ofInstant(Instant.now().minus(Duration.ofMinutes(5)).plus(duration),
                                                          Defaults.ZONE_ID));
    }

    private static ApiProvider mockApi(final OAuth oAuth) {
        final ApiProvider api = mock(ApiProvider.class);
        when(api.oauth(any())).thenAnswer(invocation -> {
            final Function<OAuth, Object> f = invocation.getArgument(0);
            return f.apply(oAuth);
        });
        return api;
    }

    @Test
    void fixesExpiredToken() {
        final OAuth oAuth = mock(OAuth.class);
        when(oAuth.login(eq(ZonkyApiToken.SCOPE_APP_WEB_STRING), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> getStaleToken());
        final ApiProvider api = mockApi(oAuth);
        final Supplier<ZonkyApiToken> t = new ZonkyApiTokenSupplier(api, SECRETS, Duration.ZERO);
        final ZonkyApiToken token = t.get();
        final ZonkyApiToken token2 = t.get();
        assertThat(token2)
                .isNotNull()
                .isNotEqualTo(token);
    }

    @Test
    void reusesExistingToken() {
        final OAuth oAuth = mock(OAuth.class);
        when(oAuth.login(eq(ZonkyApiToken.SCOPE_APP_WEB_STRING), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> getTokenExpiringIn(Duration.ofMinutes(5)));
        final ApiProvider api = mockApi(oAuth);
        final Supplier<ZonkyApiToken> t = new ZonkyApiTokenSupplier(api, SECRETS, Duration.ZERO);
        final ZonkyApiToken token = t.get();
        final ZonkyApiToken token2 = t.get();
        assertThat(token2)
                .isNotNull()
                .isEqualTo(token);
    }

    @Test
    void refreshesTokenBeforeExpiration() {
        final OAuth oAuth = mock(OAuth.class);
        when(oAuth.login(eq(ZonkyApiToken.SCOPE_APP_WEB_STRING), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenReturn(getTokenExpiringIn(Duration.ofSeconds(5)));
        when(oAuth.refresh(any()))
                .thenAnswer(invocation -> getTokenExpiringIn(Duration.ofSeconds(5)));
        final ApiProvider api = mockApi(oAuth);
        final Supplier<ZonkyApiToken> t = new ZonkyApiTokenSupplier(api, SECRETS, Duration.ofSeconds(1));
        final ZonkyApiToken token = t.get();
        final ZonkyApiToken token2 = t.get();
        assertThat(token2)
                .isNotNull()
                .isNotEqualTo(token);
        verify(oAuth).refresh(eq(token));
    }

    @Test
    void refreshFailOnToken() {
        final OAuth oAuth = mock(OAuth.class);
        when(oAuth.login(eq(ZonkyApiToken.SCOPE_APP_WEB_STRING), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> getTokenExpiringIn(Duration.ofSeconds(5)));
        when(oAuth.refresh(any()))
                .thenThrow(BadRequestException.class);
        final ApiProvider api = mockApi(oAuth);
        final Supplier<ZonkyApiToken> t = new ZonkyApiTokenSupplier(api, SECRETS, Duration.ZERO);
        final ZonkyApiToken token = t.get();
        final ZonkyApiToken token2 = t.get();
        assertThat(token2)
                .isNotNull()
                .isNotEqualTo(token);
    }

    @Test
    void refreshFailUnknown() {
        final OAuth oAuth = mock(OAuth.class);
        when(oAuth.login(eq(ZonkyApiToken.SCOPE_APP_WEB_STRING), eq(SECRETS.getUsername()), eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> getTokenExpiringIn(Duration.ofSeconds(5)));
        when(oAuth.refresh(any()))
                .thenThrow(IllegalStateException.class);
        final ApiProvider api = mockApi(oAuth);
        final Supplier<ZonkyApiToken> t = new ZonkyApiTokenSupplier(api, SECRETS, Duration.ZERO);
        assertThat(t.get()).isNotNull();
        verify(oAuth).login(any(), any(), any());
        assertThat(t.get()).isNotNull();
        verify(oAuth).refresh(any()); // refresh was called
        verify(oAuth, times(2)).login(any(), any(), any()); // password-based login was used again
    }
}
