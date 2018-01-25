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

package com.github.robozonky.app.authentication;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.BadRequestException;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class ZonkyApiTokenSupplierTest {

    private static final SecretProvider SECRETS = SecretProvider.fallback("someone", "password".toCharArray());

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
        final ApiProvider api = Mockito.mock(ApiProvider.class);
        Mockito.when(api.oauth(ArgumentMatchers.any())).thenAnswer(invocation -> {
            final Function<OAuth, Object> f = invocation.getArgument(0);
            return f.apply(oAuth);
        });
        return api;
    }

    @Test
    public void fixesExpiredToken() {
        final OAuth oAuth = Mockito.mock(OAuth.class);
        Mockito.when(oAuth.login(ArgumentMatchers.eq(SECRETS.getUsername()),
                                 ArgumentMatchers.eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> getStaleToken());
        final ApiProvider api = mockApi(oAuth);
        final Supplier<Optional<ZonkyApiToken>> t = new ZonkyApiTokenSupplier(api, SECRETS, Duration.ZERO);
        final Optional<ZonkyApiToken> token = t.get();
        Assertions.assertThat(token).isPresent();
        final Optional<ZonkyApiToken> token2 = t.get();
        Assertions.assertThat(token2).isPresent();
        Assertions.assertThat(token2).isNotEqualTo(token);
    }

    @Test
    public void reusesExistingToken() {
        final OAuth oAuth = Mockito.mock(OAuth.class);
        Mockito.when(oAuth.login(ArgumentMatchers.eq(SECRETS.getUsername()),
                                 ArgumentMatchers.eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> getTokenExpiringIn(Duration.ofMinutes(5)));
        final ApiProvider api = mockApi(oAuth);
        final Supplier<Optional<ZonkyApiToken>> t = new ZonkyApiTokenSupplier(api, SECRETS, Duration.ZERO);
        final Optional<ZonkyApiToken> token = t.get();
        Assertions.assertThat(token).isPresent();
        final Optional<ZonkyApiToken> token2 = t.get();
        Assertions.assertThat(token2).isPresent();
        Assertions.assertThat(token2).isEqualTo(token);
    }

    @Test
    public void refreshesTokenBeforeExpiration() {
        final OAuth oAuth = Mockito.mock(OAuth.class);
        Mockito.when(oAuth.login(ArgumentMatchers.eq(SECRETS.getUsername()),
                                 ArgumentMatchers.eq(SECRETS.getPassword())))
                .thenReturn(getTokenExpiringIn(Duration.ofSeconds(5)));
        Mockito.when(oAuth.refresh(ArgumentMatchers.any()))
                .thenAnswer(invocation -> getTokenExpiringIn(Duration.ofSeconds(5)));
        final ApiProvider api = mockApi(oAuth);
        final Supplier<Optional<ZonkyApiToken>> t = new ZonkyApiTokenSupplier(api, SECRETS, Duration.ofSeconds(1));
        final Optional<ZonkyApiToken> token = t.get();
        Assertions.assertThat(token).isPresent();
        final Optional<ZonkyApiToken> token2 = t.get();
        Assertions.assertThat(token2).isPresent();
        Assertions.assertThat(token2).isNotEqualTo(token);
    }

    @Test
    public void refreshFailOnToken() {
        final OAuth oAuth = Mockito.mock(OAuth.class);
        Mockito.when(oAuth.login(ArgumentMatchers.eq(SECRETS.getUsername()),
                                 ArgumentMatchers.eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> getTokenExpiringIn(Duration.ofSeconds(5)));
        Mockito.when(oAuth.refresh(ArgumentMatchers.any()))
                .thenThrow(BadRequestException.class);
        final ApiProvider api = mockApi(oAuth);
        final Supplier<Optional<ZonkyApiToken>> t = new ZonkyApiTokenSupplier(api, SECRETS, Duration.ZERO);
        final Optional<ZonkyApiToken> token = t.get();
        Assertions.assertThat(token).isPresent();
        final Optional<ZonkyApiToken> token2 = t.get();
        Assertions.assertThat(token2).isPresent();
        Assertions.assertThat(token2).isNotEqualTo(token);
    }

    @Test
    public void refreshFailUnknown() {
        final OAuth oAuth = Mockito.mock(OAuth.class);
        Mockito.when(oAuth.login(ArgumentMatchers.eq(SECRETS.getUsername()),
                                 ArgumentMatchers.eq(SECRETS.getPassword())))
                .thenAnswer(invocation -> getTokenExpiringIn(Duration.ofSeconds(5)));
        Mockito.when(oAuth.refresh(ArgumentMatchers.any()))
                .thenThrow(IllegalStateException.class);
        final ApiProvider api = mockApi(oAuth);
        final Supplier<Optional<ZonkyApiToken>> t = new ZonkyApiTokenSupplier(api, SECRETS, Duration.ZERO);
        final Optional<ZonkyApiToken> token = t.get();
        Assertions.assertThat(token).isPresent();
        final Optional<ZonkyApiToken> token2 = t.get();
        Assertions.assertThat(token2).isEmpty();
    }
}
