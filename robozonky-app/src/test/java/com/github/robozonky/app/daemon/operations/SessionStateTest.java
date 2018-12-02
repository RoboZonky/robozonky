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

package com.github.robozonky.app.daemon.operations;

import java.util.Collections;
import java.util.UUID;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Session state")
class SessionStateTest extends AbstractZonkyLeveragingTest {

    private static final String INITIAL_ID = UUID.randomUUID().toString();
    private static final Loan INITIAL_LOAN = Loan.custom().setId(1).build();
    private static final Tenant TENANT = mockTenant();
    private SessionState<Loan> state;

    @BeforeEach
    void newState() {
        state = new SessionState<>(TENANT, Collections.singleton(INITIAL_LOAN), Loan::getId, INITIAL_ID);
        state.put(INITIAL_LOAN);
    }

    @Test
    @DisplayName("stores elements.")
    void putIntoEmpty() {
        assertThat(state.contains(INITIAL_LOAN)).isTrue();
    }

    @Nested
    @DisplayName("new instance with different key")
    class InterferenceTest {

        @Test
        @DisplayName("does not interfere with original.")
        void createDifferentAndMakeSureIsEmpty() {
            final SessionState<Loan> another = new SessionState<>(TENANT, Collections.singleton(INITIAL_LOAN),
                                                                  Loan::getId, INITIAL_ID.substring(1));
            assertThat(another.contains(INITIAL_LOAN)).isFalse();
        }
    }

    @Nested
    @DisplayName("new instance with same key")
    class WhenIncludesTest {

        @Test
        @DisplayName("reads what original stored.")
        void createSameAndMakeSureHasSame() {
            final SessionState<Loan> another = new SessionState<>(TENANT, Collections.singleton(INITIAL_LOAN),
                                                                  Loan::getId, INITIAL_ID);
            assertThat(another.contains(INITIAL_LOAN)).isTrue();
        }
    }

    @Nested
    @DisplayName("clean new instance with same key")
    class WhenNoneRetainedTest {

        @Test
        @DisplayName("does not contain what was stored before.")
        void createSameAndMakeSureIsEmpty() {
            final SessionState<Loan> another = new SessionState<>(TENANT, Loan::getId, INITIAL_ID);
            assertThat(another.contains(INITIAL_LOAN)).isFalse();
        }
    }
}
