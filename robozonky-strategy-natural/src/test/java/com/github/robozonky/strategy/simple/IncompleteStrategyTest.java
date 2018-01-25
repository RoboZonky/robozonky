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

package com.github.robozonky.strategy.simple;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.test.IoTestUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class IncompleteStrategyTest {

    private static final InputStream PROPER = IncompleteStrategyTest.class.getResourceAsStream("strategy-sample.cfg");

    private static String fileToString(final File f) throws IOException {
        return Files.readAllLines(f.toPath(), Defaults.CHARSET).stream().collect(
                Collectors.joining(System.lineSeparator()));
    }

    private static void propertyIsMissing(final File strategyFile) throws IOException {
        final Optional<InvestmentStrategy> result = new SimpleStrategyService().toInvest(fileToString(strategyFile));
        Assertions.assertThat(result).isEmpty();
    }

    private static File writeToNewTempFile(final List<String> lines) {
        try {
            final File tmp = File.createTempFile("robozonky-", ".cfg");
            Files.write(tmp.toPath(), lines);
            return tmp;
        } catch (final IOException ex) {
            throw new IllegalStateException("Should not happen.", ex);
        }
    }

    @TestFactory
    Stream<DynamicTest> removeLine() throws IOException {
        final File f = IoTestUtil.streamToFile(IncompleteStrategyTest.PROPER);
        final List<String> lines = Files.lines(f.toPath()).collect(Collectors.toList());
        return IntStream.range(0, lines.size())
                .mapToObj(lineId -> {
                    final List<String> newLines = new ArrayList<>(lines);
                    final String line = newLines.remove(lineId);
                    final File tmp = writeToNewTempFile(newLines);
                    return dynamicTest("' " + line + "'", () -> propertyIsMissing(tmp));
                });
    }
}
