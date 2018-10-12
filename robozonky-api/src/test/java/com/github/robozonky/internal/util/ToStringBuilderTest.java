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

package com.github.robozonky.internal.util;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ToStringBuilderTest {

    // will be ignored by default
    private static final Logger LOGGER = LoggerFactory.getLogger(ToStringBuilderTest.class);
    // will be ignored since specifically excluded
    private final LazyInitialized<String> toString = ToStringBuilder.createFor(this, "toString");

    @Test
    void check() {
        final String s = toString.get();
        assertSoftly(softly -> {
            softly.assertThat(s).contains(this.getClass().getCanonicalName());
            softly.assertThat(s).doesNotContain("toString");
            softly.assertThat(s).doesNotContain("LOGGER");
        });
    }
}
