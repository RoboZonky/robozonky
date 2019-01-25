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

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.common.tenant.Tenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TenantBuilderTest extends AbstractZonkyLeveragingTest {

    @Test
    void apiProvided() {
        final OAuth o = mock(OAuth.class);
        when(o.login(any(), any(), any())).thenReturn(mock(ZonkyApiToken.class));
        final Zonky z = harmlessZonky(10_000);
        final ApiProvider a = mockApiProvider(o, z);
        final SecretProvider s = SecretProvider.inMemory("user", "pwd".toCharArray());
        final Tenant t = new TenantBuilder()
                .withApi(a)
                .withSecrets(s)
                .build();
        assertThat(t.getRestrictions()).isNotNull();
        verify(o).login(any(), eq(s.getUsername()), eq(s.getPassword()));
        verify(z).getRestrictions();
    }

    @Test
    void filledSessionInfo() {
        final SecretProvider s = SecretProvider.inMemory("user", "pwd".toCharArray());
        final Tenant t = new TenantBuilder()
                .withSecrets(s)
                .named("name")
                .dryRun()
                .build();
        final SessionInfo i = t.getSessionInfo();
        assertSoftly(softly -> {
            softly.assertThat(i.getUsername()).isEqualTo(s.getUsername());
            softly.assertThat(i.getName()).isEqualTo("RoboZonky 'name'");
            softly.assertThat(i.isDryRun()).isTrue();
        });
    }

    @Test
    void emptySessionInfo() {
        final SecretProvider s = SecretProvider.inMemory("user", "pwd".toCharArray());
        final Tenant t = new TenantBuilder()
                .withSecrets(s)
                .build();
        final SessionInfo i = t.getSessionInfo();
        assertSoftly(softly -> {
            softly.assertThat(i.getUsername()).isEqualTo(s.getUsername());
            softly.assertThat(i.getName()).isEqualTo("RoboZonky");
            softly.assertThat(i.isDryRun()).isFalse();
        });
    }

    @Test
    void secretsNotProvided() {
        assertThatThrownBy(() -> new TenantBuilder().build()).isInstanceOf(IllegalStateException.class);
    }
}
