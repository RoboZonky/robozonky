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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.enums.SellStatus;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;

class SaleCheckTest extends AbstractZonkyLeveragingTest {

    private final TenantPayload sut = new SaleCheck();
    private final Zonky zonky = harmlessZonky();
    private final Tenant tenant = mockTenant(zonky);
    private final SoldParticipationCache cache = SoldParticipationCache.forTenant(tenant);

    @Test
    void nothingIsOffered() {
        sut.accept(tenant);
    }

    @Nested
    class SomethingIsOffered {

        private final Investment soldEarlier = MockInvestmentBuilder.fresh(MockLoanBuilder.fresh(), 200)
            .set(InvestmentImpl::setSellStatus, SellStatus.SELLABLE_WITH_FEE)
            .build();
        private final Investment soldNow = MockInvestmentBuilder.fresh(MockLoanBuilder.fresh(), 200)
            .set(InvestmentImpl::setSellStatus, SellStatus.SOLD)
            .build();
        private final Investment onSmp = MockInvestmentBuilder.fresh(MockLoanBuilder.fresh(), 200)
            .set(InvestmentImpl::setSellStatus, SellStatus.OFFERED)
            .build();

        @BeforeEach
        void markInvestmentsAsOffered() {
            when(zonky.getInvestment(eq(soldEarlier.getId()))).thenReturn(soldEarlier);
            cache.markAsOffered(soldEarlier.getId());
            when(zonky.getInvestment(eq(soldNow.getId()))).thenReturn(soldNow);
            cache.markAsOffered(soldNow.getId());
            when(zonky.getInvestment(eq(onSmp.getId()))).thenReturn(onSmp);
            cache.markAsOffered(onSmp.getId());
        }

        @Test
        void process() {
            sut.accept(tenant);
            assertSoftly(softly -> {
                // "regular" not sold and thus not offered; "sold" was sold; "4" invalid and thus no longer offered
                assertThat(cache.getOffered()).containsOnly(onSmp.getId());
                // this was the only one sold during this session
                assertThat(cache.wasOnceSold(soldNow.getId())).isTrue();
                // this was offered, but not marked as sold == sold in some previous session
                assertThat(cache.wasOnceSold(soldEarlier.getId())).isFalse();
                // this is still on the marketplace, offered to be sold
                assertThat(cache.wasOnceSold(onSmp.getId())).isFalse();
            });
            final List<Event> events = getEventsRequested();
            assertThat(events)
                .hasSize(1)
                .first()
                .isInstanceOf(InvestmentSoldEvent.class);
        }

    }
}
