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

package com.github.robozonky.notifications.listeners;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UtilTest {

    @Test
    void emailObfuscation() {
        assertThat(Util.obfuscateEmailAddress("someone@somewhere.net")).isEqualTo("s...e@s...t");
        assertThat(Util.obfuscateEmailAddress("ab@cd")).isEqualTo("a...b@c...d");
        // too short to obfuscate
        assertThat(Util.obfuscateEmailAddress("a@b")).isEqualTo("a@b");
    }

    @Test
    void stackTrace() {
        final String result = Util.stackTraceToString(new IllegalStateException());
        assertThat(result).contains("IllegalStateException");
    }
}
