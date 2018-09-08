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

package com.github.robozonky.installer;

import java.util.Properties;
import java.util.UUID;

import com.github.robozonky.internal.util.Maps;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtilTest {

    @Test
    void testCopyOptions() {
        final String key = UUID.randomUUID().toString();
        final String[] values = new String[]{"a", "b", "c"};
        final CommandLinePart source = new CommandLinePart();
        source.setOption(key, values);
        final CommandLinePart target = new CommandLinePart();
        Util.copyOptions(source, target);
        assertThat(target.getOptions().get(key)).containsExactly(values);
    }

    @Test
    void processCommandLine() {
        final CommandLinePart part = new CommandLinePart().setProperty("someNullProperty", null); // will be skipped
        // ends up in properties
        final CommandLinePart part2 = new CommandLinePart().setProperty("robozonky.something", "somethingElse");
        // ends up on the command line
        final CommandLinePart part3 = new CommandLinePart().setProperty("something", "somethingCompletelyDifferent");
        final CommandLinePart target = new CommandLinePart();
        final Properties settings = new Properties();
        Util.processCommandLine(target, settings, part, part2, part3);
        assertThat(settings).containsOnly(Maps.entry("robozonky.something", "somethingElse"));
        assertThat(target.getProperties()).containsOnly(Maps.entry("something", "somethingCompletelyDifferent"));
    }
}
