/*
 * Copyright 2020 The RoboZonky Project
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

import static com.github.robozonky.app.tenant.ZonkyApiTokenSupplier.reloadAfter;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.remote.OAuth;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.ZonkyApiTokenImpl;
import com.github.robozonky.internal.secrets.SecretProvider;
import com.github.robozonky.internal.test.DateUtil;

class ZonkyApiTokenSupplierTest extends AbstractZonkyLeveragingTest {

    private final SecretProvider secrets = SecretProvider.inMemory("someone", "password".toCharArray());

    private static ZonkyApiToken getTokenExpiringIn(final Duration duration) {
        return new ZonkyApiTokenImpl(UUID.randomUUID()
            .toString(),
                UUID.randomUUID()
                    .toString(),
                OffsetDateTime.ofInstant(DateUtil.now()
                    .plus(duration), Defaults.ZONKYCZ_ZONE_ID));
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
        }).when(api)
            .run(any(), any());
        return api;
    }

    @Test
    void failsOnLogin() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        doThrow(IllegalStateException.class).when(oAuth)
            .refresh(any());
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, secrets);
        secrets.setToken(new ZonkyApiTokenImpl(UUID.randomUUID()
            .toString(),
                UUID.randomUUID()
                    .toString(),
                199));
        assertThatThrownBy(t::get).isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void failsOnLogin2() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        doThrow(NotAuthorizedException.class).when(oAuth)
            .refresh(any());
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, secrets);
        secrets.setToken(new ZonkyApiTokenImpl(UUID.randomUUID()
            .toString(),
                UUID.randomUUID()
                    .toString(),
                199));
        assertThatThrownBy(t::get).isInstanceOf(NotAuthorizedException.class);
    }

    @Test
    void failsOnLogin3() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        doThrow(BadRequestException.class).when(oAuth)
            .refresh(any());
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, secrets);
        secrets.setToken(new ZonkyApiTokenImpl(UUID.randomUUID()
            .toString(),
                UUID.randomUUID()
                    .toString(),
                199));
        assertThatThrownBy(t::get).isInstanceOf(BadRequestException.class);
    }

    @Test
    void failsOnRefresh() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        doThrow(IllegalStateException.class).when(oAuth)
            .refresh(any());
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, secrets);
        assertThatThrownBy(t::get).isInstanceOf(NotAuthorizedException.class);
    }

    @Test
    void closingNeverLoaded() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        final ZonkyApiToken token = getTokenExpiringIn(Duration.ofSeconds(5));
        when(oAuth.refresh(any())).thenReturn(token);
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, secrets);
        t.close();
        assertThatThrownBy(t::get).isInstanceOf(NotAuthorizedException.class);
        assertThat(t.isClosed()).isTrue();
    }

    @Test
    void notClosingWhenExpired() {
        final Zonky zonky = mock(Zonky.class);
        final OAuth oAuth = mock(OAuth.class);
        final ZonkyApiToken token = getTokenExpiringIn(Duration.ZERO);
        when(oAuth.refresh(any())).thenAnswer(invocation -> token);
        final ApiProvider api = mockApi(oAuth, zonky);
        final ZonkyApiTokenSupplier t = new ZonkyApiTokenSupplier(api, secrets);
        t.close();
    }

    @Test
    void reloadInterval() {
        var token = getTokenExpiringIn(Duration.ofMinutes(119));
        assertThat(reloadAfter(token)).isBetween(Duration.ofMinutes(59), Duration.ofMinutes(60));
        var token2 = getTokenExpiringIn(Duration.ofHours(2));
        assertThat(reloadAfter(token2)).isEqualTo(Duration.ofHours(1));
        var token3 = getTokenExpiringIn(Duration.ofMinutes(121));
        assertThat(reloadAfter(token3)).isBetween(Duration.ofMinutes(60), Duration.ofMinutes(61));
    }
}
