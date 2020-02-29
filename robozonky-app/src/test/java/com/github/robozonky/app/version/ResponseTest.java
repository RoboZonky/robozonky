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

package com.github.robozonky.app.version;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ResponseTest {

    @Test
    void equality() {
        final Response r = Response.noMoreRecentVersion();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r).isNotEqualTo(null);
            softly.assertThat(r).isEqualTo(r);
            softly.assertThat(r).isEqualTo(Response.noMoreRecentVersion());
        });
        final Response r2 = Response.moreRecentStable("5.7.0");
        final Response r3 = Response.moreRecentStable("5.8.0");
        assertThat(r2).isNotEqualTo(r3);
    }

}
