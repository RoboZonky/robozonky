/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.configuration;

import java.util.Optional;

import com.github.robozonky.app.configuration.daemon.DaemonInvestmentMode;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class CommandLineTest extends AbstractRoboZonkyTest {

    @Test
    public void properScriptIdentification() {
        System.setProperty("os.name", "Some Windows System");
        Assertions.assertThat(CommandLine.getScriptIdentifier()).isEqualTo("robozonky.bat");
        System.setProperty("os.name", "Any Other System");
        Assertions.assertThat(CommandLine.getScriptIdentifier()).isEqualTo("robozonky.sh");
    }

    @Test
    public void validDaemonCli() {
        // will fail since inside AuthenticationCommandLineFragment, -u and -g are exclusive
        final Optional<InvestmentMode> cfg = CommandLine.parse((t) -> {
                                                               }, "-u", "someone", "-p", "password",
                                                               "daemon", "-s", "somewhere");
        Assertions.assertThat(cfg).isPresent().containsInstanceOf(DaemonInvestmentMode.class);
    }

    @Test
    public void helpCli() {
        final Optional<InvestmentMode> cfg = CommandLine.parse((t) -> {
        }, "-h");
        Assertions.assertThat(cfg).isEmpty(); // would have called System.exit(), but we prevented that
    }

    @Test
    public void invalidCli() {
        final Optional<InvestmentMode> cfg = CommandLine.parse((t) -> {
        });
        Assertions.assertThat(cfg).isEmpty(); // would have called System.exit(), but we prevented that
    }
}
