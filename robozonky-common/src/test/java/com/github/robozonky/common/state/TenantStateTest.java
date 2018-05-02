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

import com.github.robozonky.api.SessionInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class TenantStateTest {

    private static final SessionInfo SESSION = new SessionInfo("someone@robozonky.cz");

    @BeforeEach
    @AfterEach
    void deleteState() {
        TenantState.destroyAll();
    }

    @Test
    void correctInstanceManagement() {
        final TenantState ts = TenantState.of(SESSION);
        final TenantState ts2 = TenantState.of(new SessionInfo(SESSION.getUsername()));
        TenantState.destroyAll();
        assertSoftly(softly -> {
            assertThat(ts).isSameAs(ts2);
            softly.assertThat(ts.isDestroyed()).isTrue();
            softly.assertThat(ts2.isDestroyed()).isTrue();
        });
        final TenantState ts3 = TenantState.of(SESSION);
        ts3.destroy();
        assertSoftly(softly -> {
            softly.assertThat(ts3)
                    .isNotSameAs(ts)
                    .isNotSameAs(ts2);
            softly.assertThat(ts3.isDestroyed()).isTrue();
        });
        final TenantState ts4 = TenantState.of(SESSION);
        assertSoftly(softly -> softly.assertThat(ts4)
                .isNotSameAs(ts)
                .isNotSameAs(ts2)
                .isNotSameAs(ts3));
    }

    @Test
    void destroysProperly() {
        final TenantState s = new TenantState("someone@robozonky.cz", mock(StateStorage.class));
        final InstanceState<TenantStateTest> cats = s.in(TenantStateTest.class);
        assertThat(cats).isNotNull();
        cats.reset(); // will succeed
        s.destroy();
        assertSoftly(softly -> { // can not succeed once destroyed
            softly.assertThatThrownBy(() -> s.in(TenantStateTest.class)).isInstanceOf(IllegalStateException.class);
            softly.assertThatThrownBy(cats::reset).isInstanceOf(IllegalStateException.class);
        });
    }
}
