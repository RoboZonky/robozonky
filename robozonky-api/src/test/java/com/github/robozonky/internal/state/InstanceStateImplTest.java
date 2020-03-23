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
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.robozonky.api.SessionInfo;

class InstanceStateImplTest {

    private static final SessionInfo SESSION = new SessionInfo("someone@robozonky.cz");

    private final InstanceStateImpl<InstanceStateImplTest> s = (InstanceStateImpl<InstanceStateImplTest>) TenantState
        .of(SESSION)
        .in(InstanceStateImplTest.class);

    @AfterEach
    void deleteState() {
        TenantState.destroyAll();
    }

    @Test
    void startFresh() {
        assertThat(s.isInitialized()).isFalse();
        s.reset();
        assertSoftly(softly -> {
            softly.assertThat(s.getKeys())
                .isEmpty();
            softly.assertThat(s.getValue("key"))
                .isEmpty();
            softly.assertThat(s.getLastUpdated())
                .isNotEmpty();
            softly.assertThat(s.isInitialized())
                .isTrue();
        });
    }

    @Test
    void startWithExisting() {
        s.update(m -> m.put("key", Stream.of("value", "value2"))
            .remove("key2"));
        assertThat(s.getValues("key")).isPresent();
        final Collection<String> values = s.getValues("key")
            .get()
            .collect(Collectors.toSet());
        assertThat(values).containsOnly("value", "value2");
        s.update(m -> m.remove("key")
            .put("key2", "value2"));
        assertSoftly(softly -> {
            softly.assertThat(s.getKeys())
                .containsOnly("key2");
            softly.assertThat(s.getValue("key"))
                .isEmpty();
            softly.assertThat(s.getValue("key2"))
                .contains("value2");
            softly.assertThat(s.getLastUpdated())
                .isNotEmpty();
        });
    }
}
