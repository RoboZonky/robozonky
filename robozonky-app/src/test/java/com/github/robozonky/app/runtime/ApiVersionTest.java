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

package com.github.robozonky.app.runtime;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ApiVersionTest {

    static final String SAMPLE = "{\"branch\":\"origin/master\"," +
            "\"commitId\":\"e51d4fcb9eac1a9599a64c93c181325a2c38e779\"," +
            "\"commitIdAbbrev\":\"e51d4fc\"," +
            "\"buildTime\":\"2018-01-18T20:16:08+0100\"," +
            "\"buildVersion\":\"0.77.0\"," +
            "\"currentApiTime\":\"2018-01-18T20:16:08.123+01:00\"," +
            "\"tags\":[\"0.77.0\"]}";

    @Test
    void parse() throws IOException {
        final ApiVersion v = ApiVersion.read(SAMPLE);
        assertSoftly(softly -> {
            softly.assertThat(v.getBranch()).isEqualTo("origin/master");
            softly.assertThat(v.getCommitId()).isEqualTo("e51d4fcb9eac1a9599a64c93c181325a2c38e779");
            softly.assertThat(v.getCommitId()).startsWith(v.getCommitIdAbbrev());
            softly.assertThat(v.getBuildVersion()).isEqualTo("0.77.0");
            softly.assertThat(v.getBuildTime()).isBefore(OffsetDateTime.now());
            softly.assertThat(v.getTags()).containsOnly("0.77.0");
            softly.assertThat(v.getCurrentApiTime()).isBeforeOrEqualTo(OffsetDateTime.now());
        });
    }
}
