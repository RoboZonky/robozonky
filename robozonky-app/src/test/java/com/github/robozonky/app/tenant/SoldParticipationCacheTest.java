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

import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.common.tenant.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

class SoldParticipationCacheTest extends AbstractZonkyLeveragingTest {

    @BeforeEach
    void reset() {
        SoldParticipationCache.resetAll();
    }

    @Test
    void persistent() {
        final SecretProvider sp = SecretProvider.inMemory("someone@somewhere.cz");
        final Tenant tenant = new TenantBuilder().withSecrets(sp).build();
        final SoldParticipationCache instance = SoldParticipationCache.forTenant(tenant);
        final SoldParticipationCache instance2 = SoldParticipationCache.forTenant(tenant);
        assertThat(instance2).isSameAs(instance);
    }

    @Test
    void tenantSpecific() {
        final SecretProvider sp = SecretProvider.inMemory("someone@somewhere.cz");
        final Tenant tenant = new TenantBuilder().withSecrets(sp).build();
        final SecretProvider sp2 = SecretProvider.inMemory("someoneElse@somewhere.cz");
        final Tenant tenant2 = new TenantBuilder().withSecrets(sp2).build();
        final SoldParticipationCache instance = SoldParticipationCache.forTenant(tenant);
        final SoldParticipationCache instance2 = SoldParticipationCache.forTenant(tenant2);
        assertThat(instance2).isNotSameAs(instance);
    }

    @Test
    void retrieves() {
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final Investment i1 = Investment.custom().setLoanId(2).build();
        when(zonky.getInvestments(notNull())).thenReturn(Stream.of(i1));
        final SoldParticipationCache instance = SoldParticipationCache.forTenant(tenant);
        assertThat(instance.wasOnceSold(2)).isTrue();
        assertThat(instance.wasOnceSold(1)).isFalse();
        instance.markAsSold(1);
        assertThat(instance.wasOnceSold(2)).isTrue();
        assertThat(instance.wasOnceSold(1)).isTrue();
    }
}
