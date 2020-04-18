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

package com.github.robozonky.internal.state;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.robozonky.api.SessionInfo;

@ExtendWith(MockitoExtension.class)
class TenantStateTest {

    @Mock(lenient = true)
    private SessionInfo sessionInfo;

    @BeforeEach
    void mockSession() {
        when(sessionInfo.getUsername()).thenReturn("someone@somewhere.cz");
    }

    @BeforeEach
    @AfterEach
    void deleteState() {
        TenantState.destroyAll();
    }

    @Test
    void correctInstanceManagement() {
        final TenantState ts = TenantState.of(sessionInfo);
        assertThat(TenantState.getKnownTenants()).containsOnly(sessionInfo);
        final TenantState ts2 = TenantState.of(sessionInfo);
        assertThat(ts2).isSameAs(ts);
        final TenantState ts3 = TenantState.of(mock(SessionInfo.class));
        assertThat(ts3).isNotSameAs(ts);
    }

    @Test
    void destroyWorks() {
        final TenantState ts = TenantState.of(sessionInfo);
        final InstanceState<TenantStateTest> is = ts.in(TenantStateTest.class);
        is.update(m -> m.put("a", "b"));
        TenantState.destroyAll();
        assertThat(TenantState.getKnownTenants()).isEmpty();
        assertThat(is.getValue("a")).isEmpty();
    }

    @Test
    void encode() {
        final String hashed = TenantState.encode("The quick brown fox jumps over the lazy dog.");
        assertThat(hashed).isEqualTo("e4d909c290d0fb1ca068ffaddf22cbd0");
    }
}
