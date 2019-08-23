/*
 * Copyright 2019 The RoboZonky Project
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

import java.util.List;
import java.util.Optional;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

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

        private final Investment soldEarlier = Investment.fresh(Loan.custom().build(), 200)
                .setOnSmp(false)
                .setStatus(InvestmentStatus.ACTIVE)
                .build();
        private final Investment soldNow = Investment.fresh(Loan.custom().build(), 200)
                .setOnSmp(false)
                .setStatus(InvestmentStatus.SOLD)
                .build();
        private final Investment onSmp = Investment.fresh(Loan.custom().build(), 200)
                .setStatus(InvestmentStatus.ACTIVE)
                .setOnSmp(true)
                .build();

        @BeforeEach
        void markInvestmentsAsOffered() {
            when(zonky.getInvestmentByLoanId(eq(soldEarlier.getLoanId()))).thenReturn(Optional.of(soldEarlier));
            cache.markAsOffered(soldEarlier.getLoanId());
            when(zonky.getInvestmentByLoanId(eq(soldNow.getLoanId()))).thenReturn(Optional.of(soldNow));
            cache.markAsOffered(soldNow.getLoanId());
            when(zonky.getInvestmentByLoanId(eq(onSmp.getLoanId()))).thenReturn(Optional.of(onSmp));
            cache.markAsOffered(onSmp.getLoanId());
            cache.markAsOffered(4); // non-existent
        }

        @Test
        void process() {
            sut.accept(tenant);
            assertSoftly(softly -> {
                // "regular" not sold and thus not offered; "sold" was sold; "4" invalid and thus no longer offered
                assertThat(cache.getOffered()).containsOnly(onSmp.getLoanId());
                // this was the only one sold during this session
                assertThat(cache.wasOnceSold(soldNow.getLoanId())).isTrue();
                // this was offered, but not marked as sold == sold in some previous session
                assertThat(cache.wasOnceSold(soldEarlier.getLoanId())).isFalse();
                // this is still on the marketplace, offered to be sold
                assertThat(cache.wasOnceSold(onSmp.getLoanId())).isFalse();
            });
            final List<Event> events = getEventsRequested();
            assertThat(events)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(InvestmentSoldEvent.class);
        }

    }
}
