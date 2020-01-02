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

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class TenantTest {

    @Test
    void delegatesSellInfo() {
        Tenant my = spy(Tenant.class);
        my.getSellInfo(1);
        verify(my).call(any());
    }

    @Test
    void delegatesLoan() {
        Tenant my = spy(Tenant.class);
        my.getLoan(1);
        verify(my).call(any());
    }

}
