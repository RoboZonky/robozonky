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

package com.github.robozonky.installer.scripts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Function;

import com.github.robozonky.installer.CommandLinePart;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class RunScriptGeneratorTest {

    private static File getTempFile() {
        try {
            return File.createTempFile("robozonky-", ".test");
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CommandLinePart getCommandLine() {
        final CommandLinePart cli = new CommandLinePart();
        cli.setOption("daemon");
        cli.setOption("-o1", "firstValue");
        cli.setOption("-o2", "firstValue", "secondValue");
        cli.setEnvironmentVariable("SOME_ENV_VAR", "someValue");
        cli.setJvmArgument("Xmx512m");
        cli.setJvmArgument("-add-modules", "java.xml.bind");
        cli.setProperty("com.github.someProperty", "someValue");
        return cli;
    }

    private static void common(final CommandLinePart cli, final String result) {
        // assert all jvm arguments present
        assertSoftly(softly -> cli.getJvmArguments().forEach((var, value) -> {
            if (value.isPresent()) {
                softly.assertThat(result).as("Missing JVM arg.").contains("-" + var + " " + value.get());
            } else {
                softly.assertThat(result).as("Missing JVM arg.").contains("-" + var);
            }
        }));
        // assert all system properties present
        assertSoftly(softly -> cli.getProperties().forEach((var, value) -> {
            final String arg = "-D" + var + "=" + value;
            softly.assertThat(result).as("Missing system property.").contains(arg);
        }));
    }

    @Test
    void windows() throws IOException {
        final File optionsFile = getTempFile().getAbsoluteFile();
        final File root = optionsFile.getParentFile();
        final CommandLinePart cli = getCommandLine();
        final Function<CommandLinePart, File> generator =
                RunScriptGenerator.forWindows(root, optionsFile);
        final String result = new String(Files.readAllBytes(generator.apply(cli).toPath()));
        common(cli, result);
        assertThat(result)
                .as("Missing executable file call.")
                .contains(root + "\\robozonky.bat @" + optionsFile.getAbsolutePath());
        // assert all environment variables present
        assertSoftly(softly -> cli.getEnvironmentVariables().forEach((var, value) -> {
            final String arg = var + "=" + value;
            softly.assertThat(result).as("Missing env var.").contains(arg);
        }));
        assertThat(result).contains("\r\n");
    }

    @Test
    void unix() throws IOException {
        final File optionsFile = getTempFile().getAbsoluteFile();
        final File root = optionsFile.getParentFile();
        final CommandLinePart cli = getCommandLine();
        final Function<CommandLinePart, File> generator =
                RunScriptGenerator.forUnix(root, optionsFile);
        final String result = new String(Files.readAllBytes(generator.apply(cli).toPath()));
        common(cli, result);
        assertThat(result)
                .as("Missing executable file call.")
                .contains(root + "/robozonky.sh @" + optionsFile.getAbsolutePath());
        // assert all environment variables present
        assertSoftly(softly -> cli.getEnvironmentVariables().forEach((var, value) -> {
            final String arg = var + "=\"" + value + "\"";
            softly.assertThat(result).as("Missing env var.").contains(arg);
        }));
        assertThat(result).doesNotContain("\r\n");
    }
}
