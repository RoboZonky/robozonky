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

package com.github.triceo.robozonky.api.strategies;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import com.github.triceo.robozonky.util.IoTestUtil;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class InvestmentStrategyLocationsTest {

    private static final InputStream CORRECT_STRATEGY =
            InvestmentStrategyLocationsTest.class.getResourceAsStream("strategy-correct.cfg");
    private static final String REMOTE_STRATEGY = "https://raw.githubusercontent" +
            ".com/triceo/robozonky/master/robozonky-app/src/main/assembly/resources/robozonky-dynamic.cfg";

    @Parameterized.Parameters
    public static Collection<Object[]> getDifferentStrategies() throws IOException {
        final File strategyFile = IoTestUtil.streamToFile(InvestmentStrategyLocationsTest.CORRECT_STRATEGY, ".cfg");
        return Arrays.asList(
          new Object[] {strategyFile}, new Object[] {strategyFile.toPath().toString()},
                new Object[]{"file://" + strategyFile.toPath()},
                new Object[] {InvestmentStrategyLocationsTest.REMOTE_STRATEGY}
        );
    }

    @Parameterized.Parameter
    public Object strategy;

    @Test
    public void loadStrategy() throws InvestmentStrategyParseException, IOException {
        Optional<InvestmentStrategy> inv;
        if (strategy instanceof File) {
            inv = InvestmentStrategy.load((File)strategy);
        } else if (strategy instanceof String) {
            inv = InvestmentStrategy.load((String)strategy);
        } else {
            throw new IllegalStateException("The test is wrong.");
        }
        Assertions.assertThat(inv).isEmpty(); // no strategies are on the classpath
    }

}
