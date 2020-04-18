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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.remote.OAuth;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.secrets.SecretProvider;
import com.github.robozonky.internal.tenant.Tenant;

class TenantBuilderTest extends AbstractZonkyLeveragingTest {

    @Test
    void apiProvided() {
        final SecretProvider s = mockSecretProvider();
        final ZonkyApiToken token = s.getToken()
            .get();
        final OAuth o = mock(OAuth.class);
        when(o.refresh(any())).thenReturn(token);
        final Zonky z = harmlessZonky();
        final ApiProvider a = mockApiProvider(o, z);
        final Tenant t = new TenantBuilder()
            .withApi(a)
            .withSecrets(s)
            .build();
        assertThat(t.getSessionInfo()
            .canAccessSmp()).isTrue();
        verify(o).refresh(eq(token));
        verify(z).getRestrictions();
        verify(z).getConsents();
    }

    @Test
    void filledSessionInfo() {
        final SecretProvider s = mockSecretProvider();
        final Tenant t = new TenantBuilder()
            .withApi(mockApiProvider(s))
            .withSecrets(s)
            .named("name")
            .dryRun()
            .build();
        final SessionInfo i = t.getSessionInfo();
        assertSoftly(softly -> {
            softly.assertThat(i.getUsername())
                .isEqualTo(s.getUsername());
            softly.assertThat(i.getName())
                .isEqualTo("RoboZonky 'name'");
            softly.assertThat(i.isDryRun())
                .isTrue();
        });
    }

    private ApiProvider mockApiProvider(SecretProvider secretProvider) {
        final ZonkyApiToken token = secretProvider.getToken()
            .get();
        final OAuth o = mock(OAuth.class);
        when(o.refresh(any())).thenReturn(token);
        final Zonky z = harmlessZonky();
        final ApiProvider a = mockApiProvider(o, z);
        return a;
    }

    @Test
    void emptySessionInfo() {
        final SecretProvider s = mockSecretProvider();
        final Tenant t = new TenantBuilder().withSecrets(s)
            .withApi(mockApiProvider(s))
            .build();
        final SessionInfo i = t.getSessionInfo();
        assertSoftly(softly -> {
            softly.assertThat(i.getUsername())
                .isEqualTo(s.getUsername());
            softly.assertThat(i.getName())
                .isEqualTo("RoboZonky");
            softly.assertThat(i.isDryRun())
                .isFalse();
        });
    }

    @Test
    void secretsNotProvided() {
        assertThatThrownBy(() -> new TenantBuilder().build()).isInstanceOf(IllegalStateException.class);
    }
}
