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

package com.github.robozonky.internal.tenant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.remote.entities.SellInfoImpl;

class TenantTest {

    @Test
    void delegatesRun() {
        Tenant my = spy(Tenant.class);
        my.run(zonky -> {
            /* NOOP */ });
        verify(my).call(notNull());
    }

    @Test
    void delegatesSellInfo() {
        SellInfo result = mock(SellInfoImpl.class);
        Tenant my = spy(Tenant.class);
        when(my.call(notNull())).thenReturn(result);
        SellInfo actual = my.getSellInfo(1);
        assertThat(actual)
            .isNotNull()
            .isSameAs(result);
    }

    @Test
    void delegatesLoan() {
        Loan result = mock(LoanImpl.class);
        Tenant my = spy(Tenant.class);
        when(my.call(notNull())).thenReturn(result);
        Loan actual = my.getLoan(1);
        assertThat(actual)
            .isNotNull()
            .isSameAs(result);
    }
}
