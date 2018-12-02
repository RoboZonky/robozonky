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

package com.github.robozonky.app.daemon;

import java.time.OffsetDateTime;

import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoanCacheTest extends AbstractZonkyLeveragingTest {

    @Test
    void emptyGetLoan() {
        final LoanCache c = new LoanCache();
        final int loanId = 1;
        assertThat(c.getLoan(loanId)).isEmpty(); // nothing returned at first
        final MyInvestment mi = mock(MyInvestment.class);
        final OffsetDateTime d = OffsetDateTime.now();
        when(mi.getTimeCreated()).thenReturn(d);
        final Loan loan = Loan.custom()
                .setId(loanId)
                .setMyInvestment(mi)
                .build();
        final Zonky z = harmlessZonky(10_000);
        final Tenant t = mockTenant(z);
        when(z.getLoan(eq(loanId))).thenReturn(loan);
        assertThat(c.getLoan(loanId, t)).isEqualTo(loan); // return the freshly retrieved loan
        final Investment i = Investment.custom()
                .setLoanId(loanId)
                .build();
        assertThat(c.getLoan(i, t)).isEqualTo(loan);
    }

    @Test
    void loadLoan() {
        final LoanCache c = new LoanCache();
        final int loanId = 1;
        final Loan loan = Loan.custom()
                .setId(loanId)
                .build();
        final Zonky z = harmlessZonky(10_000);
        when(z.getLoan(eq(loanId))).thenReturn(loan);
        final Tenant t = mockTenant(z);
        assertThat(c.getLoan(loanId, t)).isEqualTo(loan); // return the freshly retrieved loan
        verify(z).getLoan(eq(loanId));
        assertThat(c.getLoan(loanId, t)).isEqualTo(loan); // this time from the cache
        verify(z, times(1)).getLoan(eq(loanId));
    }
}
