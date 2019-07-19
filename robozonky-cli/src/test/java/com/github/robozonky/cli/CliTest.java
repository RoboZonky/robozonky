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
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.*;

class CliTest {

    private final InputStream originalSystemIn = System.in;

    @AfterEach
    void restoreSysIn() {
        System.setIn(originalSystemIn);
    }

    @Test
    void noArguments() {
        final int exit = Cli.parse();
        assertThat(exit).isEqualTo(CommandLine.ExitCode.USAGE);
    }

    @Test
    void nonExistentCommand() {
        final int exit = Cli.parse(UUID.randomUUID().toString());
        assertThat(exit).isEqualTo(CommandLine.ExitCode.USAGE);
    }

    @Test
    void nonExistentOption() {
        final int exit = Cli.parse("-" + UUID.randomUUID());
        assertThat(exit).isEqualTo(CommandLine.ExitCode.USAGE);
    }

    @Test
    void basicHelp() {
        final int exit = Cli.parse("help");
        assertThat(exit).isEqualTo(CommandLine.ExitCode.OK);
    }

    @Test
    void helpWithSubcommand() {
        final int exit = Cli.parse("help", "strategy-validator");
        assertThat(exit).isEqualTo(CommandLine.ExitCode.OK);
    }

    @Test
    void correctSubcommandNoArguments() {
        final int exit = Cli.parse("strategy-validator");
        assertThat(exit).isEqualTo(CommandLine.ExitCode.USAGE);
    }

    @Test
    void correctSubcommandWrongArgument() {
        final int exit = Cli.parse("strategy-validator ", "--location=a.txt");
        assertThat(exit).isEqualTo(CommandLine.ExitCode.USAGE);
    }

    @Test
    void correctSubcommandCorrectArgumentWrongValue() {
        System.setIn(new ByteArrayInputStream("\n".getBytes()));
        final int exit = Cli.parse("strategy-validator", "--location=file:///a.txt");
        assertThat(exit).isEqualTo(CommandLine.ExitCode.SOFTWARE);
    }

    @Test
    void correctSubcommandCorrectArgumentCorrectValue() throws IOException {
        File f = File.createTempFile("robozonky-", ".tmp");
        System.setIn(new ByteArrayInputStream("\n".getBytes()));
        final int exit = Cli.parse("strategy-validator", "--location=" + f.toURI().toURL());
        assertThat(exit).isEqualTo(CommandLine.ExitCode.SOFTWARE + 1);
    }

    @Test
    void helpWithNonexistentSubcommand() {
        final int exit = Cli.parse("help", UUID.randomUUID().toString());
        assertThat(exit).isEqualTo(CommandLine.ExitCode.USAGE);
    }
}
