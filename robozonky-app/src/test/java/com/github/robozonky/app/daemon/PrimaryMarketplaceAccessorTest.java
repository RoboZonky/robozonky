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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.LastPublishedLoan;
import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrimaryMarketplaceAccessorTest extends AbstractZonkyLeveragingTest {

    @Test
    void eliminatesUselessLoans() {
        final Loan alreadyInvested = Loan.custom()
                .setRating(Rating.B)
                .setNonReservedRemainingInvestment(1)
                .setMyInvestment(mock(MyInvestment.class))
                .build();
        final Loan normal = Loan.custom()
                .setRating(Rating.A)
                .setNonReservedRemainingInvestment(1)
                .build();
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getAvailableLoans(any())).thenReturn(Stream.of(alreadyInvested, normal));
        final Tenant tenant = mockTenant(zonky);
        final MarketplaceAccessor<LoanDescriptor> d = new PrimaryMarketplaceAccessor(tenant, UnaryOperator.identity());
        final Collection<LoanDescriptor> ld = d.getMarketplace();
        assertThat(ld).hasSize(1)
                .element(0)
                .extracting(LoanDescriptor::item)
                .isSameAs(normal);
    }

    @Test
    void detectsUpdates() {
        final Zonky z = harmlessZonky(10_000);
        when(z.getLastPublishedLoanInfo()).thenReturn(mock(LastPublishedLoan.class));
        final Tenant t = mockTenant(z);
        final AtomicReference<LastPublishedLoan> state = new AtomicReference<>(null);
        final MarketplaceAccessor<LoanDescriptor> a = new PrimaryMarketplaceAccessor(t, state::getAndSet);
        assertThat(a.hasUpdates()).isTrue(); // detect update, store present state
        assertThat(a.hasUpdates()).isFalse(); // state stays the same, no update
    }

    @Test
    void failsDetection() {
        final Zonky z = harmlessZonky(10_000);
        when(z.getLastPublishedLoanInfo()).thenThrow(IllegalStateException.class);
        final Tenant t = mockTenant(z);
        final AtomicReference<LastPublishedLoan> state = new AtomicReference<>(null);
        final MarketplaceAccessor<LoanDescriptor> a = new PrimaryMarketplaceAccessor(t, state::getAndSet);
        assertThat(a.hasUpdates()).isTrue();
        assertThat(a.hasUpdates()).isTrue();
    }
}
