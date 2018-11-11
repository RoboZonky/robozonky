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

import java.io.IOException;

import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

class TokenBasedTenantTest extends AbstractRoboZonkyTest {

    private static final SecretProvider SECRETS = SecretProvider.inMemory(SESSION.getUsername());

    @Test
    void closesWhenNoTokens() {
        final OAuth a = mock(OAuth.class);
        final Zonky z = mock(Zonky.class);
        final ApiProvider api = mockApiProvider(a, z);
        try (final Tenant tenant = new TenantBuilder().withSecrets(SECRETS).withApi(api).build()) {
            assertThat(tenant.isAvailable()).isFalse();
        } catch (final IOException e) {
            fail(e);
        }
        verifyZeroInteractions(a);
        verifyZeroInteractions(z);
    }

    @Test
    void closesWithTokens() {
        final OAuth a = mock(OAuth.class);
        when(a.login(any(), any(), any())).thenReturn(mock(ZonkyApiToken.class));
        final Zonky z = harmlessZonky(10_000);
        final ApiProvider api = mockApiProvider(a, z);
        try (final Tenant tenant = new TenantBuilder().withSecrets(SECRETS).withApi(api).build()) {
            final Statistics s = tenant.call(Zonky::getStatistics);
            assertThat(s).isSameAs(Statistics.empty());
            assertThat(tenant.isAvailable()).isTrue();
        } catch (final IOException e) {
            fail(e);
        }
        verify(a).login(any(), any(), any());
        verify(z).logout();
    }
}
