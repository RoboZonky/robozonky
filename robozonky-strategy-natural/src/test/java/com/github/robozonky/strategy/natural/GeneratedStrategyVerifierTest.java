/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.strategy.natural;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

class GeneratedStrategyVerifierTest {

    private static final class StrategySourceProvider implements ArgumentsProvider {

        private File[] getResourceFolderFiles() {
            URL url = GeneratedStrategyVerifierTest.class.getResource("");
            String path = url.getPath();
            return new File(path).listFiles();
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(getResourceFolderFiles())
                .filter(f -> f.getName()
                    .startsWith("generated-"))
                .sorted(Comparator.comparing(File::getName))
                .map(f -> Arguments.of(f.toPath()
                    .toAbsolutePath()));
        }
    }

    private static String convertToString(final Path file) {
        try {
            return Files.readAllLines(file)
                .stream()
                .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @ParameterizedTest
    @ArgumentsSource(StrategySourceProvider.class)
    void strategyParsedCorrectly(Path id) {
        final String input = convertToString(id);
        final ParsedStrategy s = GeneratedStrategyVerifier.parseWithAntlr(input);
        assertThat(s)
            .as("Failed parsing strategy {}.", id)
            .isNotNull();
    }

}
