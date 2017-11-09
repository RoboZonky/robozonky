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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.test.IoTestUtil;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class IncompleteStrategyTest {

    private static final InputStream PROPER = IncompleteStrategyTest.class.getResourceAsStream("strategy-sample.cfg");

    @Parameterized.Parameters(name = "removed \"{0}\" from {1}")
    public static Collection<Object[]> getParameters() throws IOException {
        final File f = IoTestUtil.streamToFile(IncompleteStrategyTest.PROPER);
        final List<String> lines = Files.lines(f.toPath()).collect(Collectors.toList());
        final Collection<Object[]> files = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            final List<String> newLines = new ArrayList<>(lines);
            final String line = newLines.remove(i);
            final File tmp = File.createTempFile("robozonky-", ".cfg");
            Files.write(tmp.toPath(), newLines);
            files.add(new Object[]{line, tmp});
        }
        return files;
    }

    private static String fileToString(final File f) throws IOException {
        return Files.readAllLines(f.toPath(), Defaults.CHARSET).stream().collect(
                Collectors.joining(System.lineSeparator()));
    }

    @Parameterized.Parameter
    public String lineBeingRemoved;

    @Parameterized.Parameter(1)
    public File strategyFile;

    @Test
    public void propertyIsMissing() throws IOException {
        final Optional<InvestmentStrategy> result =
                new SimpleStrategyService().toInvest(fileToString(this.strategyFile));
        Assertions.assertThat(result).isEmpty();
    }
}
