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

package com.github.robozonky.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class SessionInfoTest {

    @Test
    void constructor() {
        final SessionInfo s = new SessionInfo("someone@somewhere.cz");
        assertSoftly(softly -> {
            softly.assertThat(s.getUsername()).isEqualTo("someone@somewhere.cz");
            softly.assertThat(s.isDryRun()).isTrue();
            softly.assertThat(s.getName()).isEqualTo("RoboZonky");
        });
    }

    @Test
    void constructorNamed() {
        final SessionInfo s = new SessionInfo("someone@somewhere.cz", "Test");
        assertSoftly(softly -> {
            softly.assertThat(s.getUsername()).isEqualTo("someone@somewhere.cz");
            softly.assertThat(s.isDryRun()).isTrue();
            softly.assertThat(s.getName()).isEqualTo("RoboZonky 'Test'");
        });
    }

    @Test
    void equals() {
        final SessionInfo s = new SessionInfo("someone@somewhere.cz");
        final SessionInfo s2 = new SessionInfo(UUID.randomUUID().toString());
        final SessionInfo s3 = new SessionInfo("someone@somewhere.cz");
        assertSoftly(softly -> {
            softly.assertThat(s).isNotEqualTo(null);
            softly.assertThat(s).isNotEqualTo(UUID.randomUUID().toString());
            softly.assertThat(s).isEqualTo(s);
            softly.assertThat(s).isNotEqualTo(s2);
            softly.assertThat(s).isEqualTo(s3);
        });
    }
}
