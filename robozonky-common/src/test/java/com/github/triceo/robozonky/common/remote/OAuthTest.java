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

package com.github.triceo.robozonky.common.remote;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.github.triceo.robozonky.api.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class OAuthTest {

    private static final String USERNAME = "username", PASSWORD = "password";

    @Test
    public void login() {
        final ZonkyOAuthApi api = Mockito.mock(ZonkyOAuthApi.class);
        final Api<ZonkyOAuthApi> wrapper = new Api<>(api);
        try (final OAuth oauth = new OAuth(wrapper)) {
            oauth.login(USERNAME, PASSWORD.toCharArray());
        }
        Mockito.verify(api, Mockito.times(1))
                .login(ArgumentMatchers.eq(USERNAME), ArgumentMatchers.eq(PASSWORD),
                        ArgumentMatchers.eq("password"),
                        ArgumentMatchers.eq("SCOPE_APP_WEB"));
        Assertions.assertThat(wrapper.isClosed()).isTrue();
    }

    @Test
    public void refresh() {
        final String originalTokenId = UUID.randomUUID().toString();
        final ZonkyApiToken originToken = new ZonkyApiToken(UUID.randomUUID().toString(), originalTokenId,
                OffsetDateTime.now());
        final ZonkyApiToken resultToken = Mockito.mock(ZonkyApiToken.class);
        final ZonkyOAuthApi api = Mockito.mock(ZonkyOAuthApi.class);
        Mockito.when(api.refresh(ArgumentMatchers.eq(originalTokenId),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(resultToken);
        final Api<ZonkyOAuthApi> wrapper = new Api<>(api);
        try (final OAuth oauth = new OAuth(wrapper)) {
            final ZonkyApiToken returnedToken = oauth.refresh(originToken);
            Assertions.assertThat(returnedToken).isEqualTo(resultToken);
        }
        Assertions.assertThat(wrapper.isClosed()).isTrue();
    }

}
