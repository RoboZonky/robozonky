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

package com.github.robozonky.internal.remote;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.internal.remote.endpoints.ZonkyOAuthApi;
import com.github.robozonky.internal.remote.entities.ZonkyApiTokenImpl;

class OAuthTest {

    private static final String PASSWORD = "password";

    @Test
    void login() {
        final ZonkyOAuthApi api = mock(ZonkyOAuthApi.class);
        when(api.login(anyString(), anyString(), anyString())).thenReturn(mock(ZonkyApiTokenImpl.class));
        final Api<ZonkyOAuthApi> wrapper = new Api<>(api);
        final OAuth oauth = new OAuth(wrapper);
        final ZonkyApiToken token = oauth.login(PASSWORD.toCharArray());
        assertThat(token).isNotNull();
        verify(api, times(1))
            .login(eq(PASSWORD), any(), eq("authorization_code"));
    }

    @Test
    void refresh() {
        final String originalTokenId = UUID.randomUUID()
            .toString();
        final ZonkyApiToken originToken = new ZonkyApiTokenImpl(UUID.randomUUID()
            .toString(), originalTokenId,
                                                                OffsetDateTime.now());
        final ZonkyApiTokenImpl resultToken = mock(ZonkyApiTokenImpl.class);
        final ZonkyOAuthApi api = mock(ZonkyOAuthApi.class);
        when(api.refresh(eq(originalTokenId), anyString())).thenReturn(resultToken);
        final Api<ZonkyOAuthApi> wrapper = new Api<>(api);
        final OAuth oauth = new OAuth(wrapper);
        final ZonkyApiToken returnedToken = oauth.refresh(originToken);
        assertThat(returnedToken).isEqualTo(resultToken);
    }
}
