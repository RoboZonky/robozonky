/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.configuration;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemOutRule;

public class CommandLineInterfaceTest {

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Before
    public void clearLog() {
        systemOutRule.clearLog();
    }

    @Test
    public void properScriptIdentification() {
        System.setProperty("os.name", "Some Windows System");
        Assertions.assertThat(CommandLineInterface.getScriptIdentifier()).isEqualTo("robozonky.bat");
        System.setProperty("os.name", "Any Other System");
        Assertions.assertThat(CommandLineInterface.getScriptIdentifier()).isEqualTo("robozonky.sh");
    }

    @Test
    public void helpCli() {
        final Optional<Configuration> cfg = CommandLineInterface.parse("-h");
        Assertions.assertThat(cfg).isEmpty();
        Assertions.assertThat(systemOutRule.getLog()).contains(CommandLineInterface.getScriptIdentifier());
    }

    @Test
    public void invalidFragmentCli() {
        // will fail since inside AuthenticationCommandLineFragment, -u and -g are exclusive
        final Optional<Configuration> cfg = CommandLineInterface.parse("-u", "someone", "-g", "somewhere",
                "-p", "password", "many", "-l", "somewhere");
        Assertions.assertThat(cfg).isEmpty();
        Assertions.assertThat(systemOutRule.getLog()).contains(CommandLineInterface.getScriptIdentifier());
    }

}
