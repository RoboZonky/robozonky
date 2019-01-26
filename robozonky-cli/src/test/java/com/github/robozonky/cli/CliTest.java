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

package com.github.robozonky.cli;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CliTest {

    private final InputStream originalSystemIn = System.in;

    @AfterEach
    void restoreSysIn() {
        System.setIn(originalSystemIn);
    }

    @Test
    void noArguments() {
        final Optional<ExitCode> exit = Cli.parse();
        assertThat(exit).contains(ExitCode.NO_OPERATION);
    }

    @Test
    void nonExistentCommand() {
        final Optional<ExitCode> exit = Cli.parse(UUID.randomUUID().toString());
        assertThat(exit).contains(ExitCode.NO_OPERATION);
    }

    @Test
    void nonExistentOption() {
        final Optional<ExitCode> exit = Cli.parse("-" + UUID.randomUUID());
        assertThat(exit).contains(ExitCode.NO_OPERATION);
    }

    @Test
    void basicHelp() {
        final Optional<ExitCode> exit = Cli.parse("help");
        assertThat(exit).contains(ExitCode.NO_OPERATION);
    }

    @Test
    void helpWithSubcommand() {
        final Optional<ExitCode> exit = Cli.parse("help", "strategy-validator");
        assertThat(exit).contains(ExitCode.NO_OPERATION);
    }

    @Test
    void correctSubcommandNoArguments() {
        final Optional<ExitCode> exit = Cli.parse("strategy-validator");
        assertThat(exit).contains(ExitCode.NO_OPERATION);
    }

    @Test
    void correctSubcommandWrongArgument() {
        final Optional<ExitCode> exit = Cli.parse("strategy-validator ", "--location=a.txt");
        assertThat(exit).contains(ExitCode.NO_OPERATION);
    }

    @Test
    void correctSubcommandCorrectArgumentWrongValue() {
        System.setIn(new ByteArrayInputStream("\n".getBytes()));
        final Optional<ExitCode> exit = Cli.parse("strategy-validator", "--location=file:///a.txt");
        assertThat(exit).contains(ExitCode.SETUP_FAIL);
    }

    @Test
    void correctSubcommandCorrectArgumentCorrectValue() throws IOException {
        File f = File.createTempFile("robozonky-", ".tmp");
        System.setIn(new ByteArrayInputStream("\n".getBytes()));
        final Optional<ExitCode> exit = Cli.parse("strategy-validator", "--location=" + f.toURI().toURL());
        assertThat(exit).contains(ExitCode.TEST_FAIL);
    }

    @Test
    void helpWithNonexistentSubcommand() {
        final Optional<ExitCode> exit = Cli.parse("help", UUID.randomUUID().toString());
        assertThat(exit).contains(ExitCode.NO_OPERATION);
    }
}
