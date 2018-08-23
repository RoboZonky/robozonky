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

package com.github.robozonky.integrations.stonky;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.github.robozonky.util.LazyInitialized;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UtilTest extends AbstractRoboZonkyTest {

    @Test
    void transport() {
        assertThat(Util.createTransport()).isNotNull();
    }

    @Test
    void downloadFromWrongUrl() throws MalformedURLException {
        final URL url = new URL("http://" + UUID.randomUUID());
        assertThat(Util.download(url)).isEmpty();
    }

    @Test
    void logsOut() {
        final OAuth oAuth = mock(OAuth.class);
        when(oAuth.login(any(), any(), any())).thenReturn(mock(ZonkyApiToken.class));
        final Zonky zonky = mock(Zonky.class);
        final ApiProvider api = mockApiProvider(oAuth, zonky);
        final SecretProvider secretProvider = SecretProvider.inMemory("someone@somewhere.cz");
        final LazyInitialized<ZonkyApiToken> token = Util.getToken(api, secretProvider,
                                                                   ZonkyApiToken.SCOPE_APP_WEB_STRING);
        final ZonkyApiToken actual = token.get();
        assertThat(actual).isNotNull();
        verify(oAuth).login(eq(ZonkyApiToken.SCOPE_APP_WEB_STRING), eq(secretProvider.getUsername()), any());
        token.close();
        verify(zonky).logout();
    }
}
