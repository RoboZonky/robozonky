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

package com.github.robozonky.app.daemon;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.TenantBuilder;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.secrets.SecretProvider;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.test.mock.MockInvestmentBuilder;

class SoldParticipationCacheTest extends AbstractZonkyLeveragingTest {

    @BeforeEach
    void reset() {
        SoldParticipationCache.resetAll();
    }

    @Test
    void persistent() {
        final SecretProvider sp = SecretProvider.inMemory("someone@somewhere.cz");
        final Tenant tenant = new TenantBuilder().withSecrets(sp)
            .build(false);
        final SoldParticipationCache instance = SoldParticipationCache.forTenant(tenant);
        final SoldParticipationCache instance2 = SoldParticipationCache.forTenant(tenant);
        assertThat(instance2).isSameAs(instance);
    }

    @Test
    void tenantSpecific() {
        final SecretProvider sp = SecretProvider.inMemory("someone@somewhere.cz");
        final Tenant tenant = new TenantBuilder().withSecrets(sp)
            .build(false);
        final SecretProvider sp2 = SecretProvider.inMemory("someoneElse@somewhere.cz");
        final Tenant tenant2 = new TenantBuilder().withSecrets(sp2)
            .build(false);
        final SoldParticipationCache instance = SoldParticipationCache.forTenant(tenant);
        final SoldParticipationCache instance2 = SoldParticipationCache.forTenant(tenant2);
        assertThat(instance2).isNotSameAs(instance);
    }

    @Test
    void retrievesSold() {
        final Zonky zonky = harmlessZonky();
        final Tenant tenant = mockTenant(zonky);
        final Investment i1 = MockInvestmentBuilder.fresh()
            .set(InvestmentImpl::setLoanId, 2)
            .build();
        when(zonky.getSoldInvestments()).thenReturn(Stream.of(i1));
        final SoldParticipationCache instance = SoldParticipationCache.forTenant(tenant);
        assertThat(instance.wasOnceSold(2)).isTrue();
        assertThat(instance.wasOnceSold(1)).isFalse();
        instance.markAsSold(1);
        assertThat(instance.wasOnceSold(2)).isTrue();
        assertThat(instance.wasOnceSold(1)).isTrue();
    }

    @Test
    void retrievesOffered() {
        final Zonky zonky = harmlessZonky();
        final Tenant tenant = mockTenant(zonky);
        final Investment i1 = MockInvestmentBuilder.fresh()
            .set(InvestmentImpl::setLoanId, 2)
            .build();
        when(zonky.getInvestments(notNull())).thenReturn(Stream.of(i1));
        final SoldParticipationCache instance = SoldParticipationCache.forTenant(tenant);
        assertThat(instance.getOffered()).isEmpty();
        instance.markAsOffered(1);
        instance.markAsOffered(2);
        assertThat(instance.getOffered()).containsOnly(1, 2);
        instance.markAsSold(1);
        assertSoftly(softly -> {
            softly.assertThat(instance.getOffered())
                .containsOnly(2);
            softly.assertThat(instance.wasOnceSold(1))
                .isTrue();
            softly.assertThat(instance.wasOnceSold(2))
                .isFalse();
        });
    }
}
