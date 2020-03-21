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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.LastPublishedLoan;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.test.mock.MockLoanBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestingOperationDescriptionTest extends AbstractZonkyLeveragingTest {

    @Test
    void getters() {
        final InvestingOperationDescriptor d = new InvestingOperationDescriptor();
        final Loan l = MockLoanBuilder.fresh();
        final LoanDescriptor ld = new LoanDescriptor(l);
        assertThat(d.identify(ld)).isEqualTo(l.getId());
        assertThat(d.getMinimumBalance(mockTenant())).isEqualTo(Money.from(200));
    }

    @Test
    void freshAccessorEveryTimeButTheyShareState() {
        final Zonky z = harmlessZonky();
        when(z.getLastPublishedLoanInfo()).thenReturn(mock(LastPublishedLoan.class));
        final PowerTenant t = mockTenant(z);
        final InvestingOperationDescriptor d = new InvestingOperationDescriptor();
        final AbstractMarketplaceAccessor<LoanDescriptor> a1 = d.newMarketplaceAccessor(t);
        assertThat(a1.hasUpdates()).isTrue();
        final AbstractMarketplaceAccessor<LoanDescriptor> a2 = d.newMarketplaceAccessor(t);
        assertThat(a2.hasUpdates()).isFalse();
    }

    @Test
    void enabled() {
        final Zonky z = harmlessZonky();
        final PowerTenant t = mockTenant(z);
        when(z.getRestrictions()).thenReturn(new Restrictions(false));
        final InvestingOperationDescriptor d = new InvestingOperationDescriptor();
        assertThat(d.isEnabled(t)).isFalse();
    }

}
