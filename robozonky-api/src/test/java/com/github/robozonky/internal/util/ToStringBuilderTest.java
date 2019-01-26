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

package com.github.robozonky.internal.util;

import io.vavr.Lazy;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ToStringBuilderTest {

    // will be ignored as it's static
    private static final char CHAR = '*';
    // will be ignored since specifically excluded
    private final Lazy<String> toString = Lazy.of(() -> ToStringBuilder.createFor(this, "toString"));
    // will be ignored as it's one of the ignored types
    private final Logger LOGGER = LogManager.getLogger(ToStringBuilderTest.class);
    // will be concatenated as it's too long
    private final String abbreviated = StringUtils.repeat(CHAR, 100);

    @Test
    void check() {
        final String s = toString.get();
        assertSoftly(softly -> {
            softly.assertThat(s).contains(this.getClass().getCanonicalName());
            softly.assertThat(s).contains("abbreviated=" + abbreviated.substring(0, 70 - 3) + "...");
            softly.assertThat(s).doesNotContain("toString");
            softly.assertThat(s).doesNotContain("CHAR");
            softly.assertThat(s).doesNotContain("LOGGER");
        });
    }
}
