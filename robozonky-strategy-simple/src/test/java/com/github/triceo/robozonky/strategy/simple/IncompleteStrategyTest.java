/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy.simple;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.strategy.InvestmentStrategyParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class IncompleteStrategyTest {

    private static final File PROPER =
            new File("src/test/resources/com/github/triceo/robozonky/strategy/simple/strategy-sample.cfg");

    @Parameterized.Parameters(name = "removed \"{0}\" from {1}")
    public static Collection<Object[]> getParameters() throws IOException {
        final List<String> lines = Files.lines(IncompleteStrategyTest.PROPER.toPath()).collect(Collectors.toList());
        final Collection<Object[]> files = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            final List<String> newLines = new ArrayList<>(lines);
            final String line = newLines.remove(i);
            final File tmp = File.createTempFile("robozonky-", ".cfg");
            Files.write(tmp.toPath(), newLines);
            files.add(new Object[] {line, tmp});
        }
        return files;
    }

    @Parameterized.Parameter
    public String lineBeingRemoved;

    @Parameterized.Parameter(1)
    public File strategyFile;

    @Test(expected = InvestmentStrategyParseException.class)
    public void propertyIsMissing() throws InvestmentStrategyParseException {
        new SimpleInvestmentStrategyService().parse(this.strategyFile);
    }

}
