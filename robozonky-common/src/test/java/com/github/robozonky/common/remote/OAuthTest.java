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

import java.time.OffsetDateTime;
import java.util.UUID;

import com.github.robozonky.api.remote.ZonkyOAuthApi;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthTest {

    private static final String USERNAME = "username", PASSWORD = "password";

    @Test
    void login() {
        final ZonkyOAuthApi api = mock(ZonkyOAuthApi.class);
        when(api.login(anyString(), anyString(), anyString(), anyString())).thenReturn(mock(ZonkyApiToken.class));
        final Api<ZonkyOAuthApi> wrapper = new Api<>(api);
        final OAuth oauth = new OAuth(wrapper);
        oauth.login(USERNAME, PASSWORD.toCharArray());
        verify(api, times(1))
                .login(eq(USERNAME), eq(PASSWORD),
                       eq("password"),
                       eq("SCOPE_APP_WEB"));
    }

    @Test
    void refresh() {
        final String originalTokenId = UUID.randomUUID().toString();
        final ZonkyApiToken originToken = new ZonkyApiToken(UUID.randomUUID().toString(), originalTokenId,
                                                            OffsetDateTime.now());
        final ZonkyApiToken resultToken = mock(ZonkyApiToken.class);
        final ZonkyOAuthApi api = mock(ZonkyOAuthApi.class);
        when(api.refresh(eq(originalTokenId),
                         anyString(), anyString())).thenReturn(resultToken);
        final Api<ZonkyOAuthApi> wrapper = new Api<>(api);
        final OAuth oauth = new OAuth(wrapper);
        final ZonkyApiToken returnedToken = oauth.refresh(originToken);
        assertThat(returnedToken).isEqualTo(resultToken);
    }
}
