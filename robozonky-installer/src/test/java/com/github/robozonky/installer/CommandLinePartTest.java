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

package com.github.robozonky.installer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class CommandLinePartTest {

    @Test
    void options() {
        final String firstOption = UUID.randomUUID().toString();
        final String secondOption = UUID.randomUUID().toString();
        final String thirdOption = UUID.randomUUID().toString();
        final String firstValue = UUID.randomUUID().toString();
        final String secondValue = UUID.randomUUID().toString();
        final CommandLinePart clp = new CommandLinePart()
                .setOption(firstOption)
                .setOption(secondOption, firstValue)
                .setOption(thirdOption, firstValue, secondValue)
                .setOption(secondOption, secondValue, firstValue); // override
        assertSoftly(softly -> {
            final Map<String, Collection<String>> options = clp.getOptions();
            softly.assertThat(options.get(UUID.randomUUID().toString())).isNull();
            softly.assertThat(options.get(firstOption)).isEmpty();
            softly.assertThat(options.get(secondOption)).containsExactly(secondValue, firstValue);
            softly.assertThat(options.get(thirdOption)).containsExactly(firstValue, secondValue);
            softly.assertThat(clp.convertOptions()).isNotEmpty();
            softly.assertThat(clp.getJvmArguments()).isEmpty();
            softly.assertThat(clp.getProperties()).isEmpty();
            softly.assertThat(clp.getEnvironmentVariables()).isEmpty();
        });
    }

    @Test
    void properties() {
        final String firstProperty = UUID.randomUUID().toString();
        final String secondProperty = UUID.randomUUID().toString();
        final String firstValue = UUID.randomUUID().toString();
        final String secondValue = UUID.randomUUID().toString();
        final CommandLinePart clp = new CommandLinePart()
                .setProperty(firstProperty, firstValue)
                .setProperty(secondProperty, secondValue);
        assertSoftly(softly -> {
            final Map<String, String> properties = clp.getProperties();
            softly.assertThat(properties.get(UUID.randomUUID().toString())).isNull();
            softly.assertThat(properties.get(firstProperty)).isSameAs(firstValue);
            softly.assertThat(properties.get(secondProperty)).isSameAs(secondValue);
            softly.assertThat(clp.getJvmArguments()).isEmpty();
            softly.assertThat(clp.getOptions()).isEmpty();
            softly.assertThat(clp.getEnvironmentVariables()).isEmpty();
        });
    }

    @Test
    void environmentVariables() {
        final String firstVariable = UUID.randomUUID().toString();
        final String secondVariable = UUID.randomUUID().toString();
        final String firstValue = UUID.randomUUID().toString();
        final String secondValue = UUID.randomUUID().toString();
        final CommandLinePart clp = new CommandLinePart()
                .setEnvironmentVariable(firstVariable, firstValue)
                .setEnvironmentVariable(secondVariable, secondValue);
        assertSoftly(softly -> {
            final Map<String, String> variables = clp.getEnvironmentVariables();
            softly.assertThat(variables.get(UUID.randomUUID().toString())).isNull();
            softly.assertThat(variables.get(firstVariable)).isSameAs(firstValue);
            softly.assertThat(variables.get(secondVariable)).isSameAs(secondValue);
            softly.assertThat(clp.getJvmArguments()).isEmpty();
            softly.assertThat(clp.getOptions()).isEmpty();
            softly.assertThat(clp.getProperties()).isEmpty();
        });
    }

    @Test
    void jvmArguments() {
        final String firstVariable = UUID.randomUUID().toString();
        final String secondVariable = UUID.randomUUID().toString();
        final String firstValue = UUID.randomUUID().toString();
        final CommandLinePart clp = new CommandLinePart()
                .setJvmArgument(firstVariable, firstValue)
                .setJvmArgument(secondVariable);
        assertSoftly(softly -> {
            final Map<String, Optional<String>> variables = clp.getJvmArguments();
            softly.assertThat(variables.get(UUID.randomUUID().toString())).isNull();
            softly.assertThat(variables.get(firstVariable)).isPresent().contains(firstValue);
            softly.assertThat(variables.get(secondVariable)).isEmpty();
            softly.assertThat(clp.getEnvironmentVariables()).isEmpty();
            softly.assertThat(clp.getOptions()).isEmpty();
            softly.assertThat(clp.getProperties()).isEmpty();
        });
    }

    @Test
    void optionStorage() throws IOException {
        final File target = File.createTempFile("robozonky-", ".cli");
        new CommandLinePart()
                .setOption("-noarg")
                .setOption("-onearg", "a1")
                .setOption("-twoarg", "a2", "a3")
                .storeOptions(target);
        final List<String> result = Files.readAllLines(target.toPath());
        assertThat(result).containsExactly("-noarg", "-onearg", "\"a1\"", "-twoarg", "\"a2\"", "\"a3\"");
    }
}
