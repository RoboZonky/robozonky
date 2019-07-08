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

package com.github.robozonky.app.summaries;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.tenant.Tenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class LoanAndInvestmentImplTest extends AbstractZonkyLeveragingTest {

    @Test
    void basics() {
        final Loan l = Loan.custom().build();
        final Tenant t = mockTenant();
        doReturn(l).when(t).getLoan(anyInt());
        final Investment i = Investment.fresh(l, 200).build();
        final LoanAndInvestmentImpl li = new LoanAndInvestmentImpl(i, t::getLoan);
        assertSoftly(softly -> {
            softly.assertThat(li.getInvestment()).isEqualTo(i);
            softly.assertThat(li.getLoan()).isEqualTo(l);
        });
    }
}
