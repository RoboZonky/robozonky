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

package com.github.robozonky.common.state;

import java.time.OffsetDateTime;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.common.secrets.SecretProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StateCleanerTest {

    private static final SessionInfo SESSION_INFO = new SessionInfo("someone@somewhere.cz");

    @AfterEach
    void destroyAllState() {
        TenantState.destroyAll();
    }

    @Test
    void cleansOverThreshold() {
        TenantState.of(SESSION_INFO).in(StateCleaner.class).update(m -> m.put("a", "b").put("b", "c"));
        TenantState.of(SESSION_INFO).in(SessionInfo.class).update(m -> m.put("c", "d"));
        final StateCleaner stateCleaner = new StateCleaner(OffsetDateTime.now().plusDays(1)); // delete everything
        stateCleaner.accept(SecretProvider.inMemory(SESSION_INFO.getUsername()));
        assertThat(TenantState.of(SESSION_INFO).in(StateCleaner.class).getLastUpdated()).isEmpty();
        assertThat(TenantState.of(SESSION_INFO).in(SessionInfo.class).getLastUpdated()).isEmpty();
    }
}
