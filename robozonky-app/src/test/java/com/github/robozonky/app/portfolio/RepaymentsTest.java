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

package com.github.robozonky.app.portfolio;

import java.util.Collections;

import com.github.robozonky.api.notifications.LoanRepaidEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RepaymentsTest extends AbstractZonkyLeveragingTest {

    @Test
    void registerNewRepayments() {
        final Loan l = spy(new Loan(1, 1000));
        when(l.getRemainingInvestment()).thenReturn(0.0);
        final Investment i = spy(new Investment(l, 200));
        // first, portfolio contains one active investment; no repaid
        final Portfolio p = new Portfolio(Collections.singletonList(i));
        final Zonky z = harmlessZonky(10_000);
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        final Authenticated a = mockAuthentication(z);
        final Repayments r = new Repayments(true);
        r.accept(p, a);
        assertThat(getNewEvents()).isEmpty();
        // now, portfolio has the same investment marked as paid; event will be triggered
        when(i.getPaymentStatus()).thenReturn(PaymentStatus.PAID);
        r.accept(p, a);
        assertThat(getNewEvents())
                .first()
                .isInstanceOf(LoanRepaidEvent.class);
        // make sure the loan was retrieved from Zonky
        verify(z).getLoan(eq(l.getId()));
    }
}
