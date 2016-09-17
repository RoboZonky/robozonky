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

package com.github.triceo.robozonky.app;

import java.io.File;
import java.util.Optional;

import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import com.github.triceo.robozonky.strategy.InvestmentStrategyParseException;
import com.github.triceo.robozonky.util.IoTestUtil;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class StrategyLoadingTest {

    private static String getRoot() {
        return IoTestUtil.findMainSource("assembly", "resources");
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new File[][] {
            new File[] {new File(StrategyLoadingTest.getRoot(), "robozonky-balanced.cfg")},
                new File[] {new File(StrategyLoadingTest.getRoot(), "robozonky-conservative.cfg")},
                new File[] {new File(StrategyLoadingTest.getRoot(), "robozonky-dynamic.cfg")}
        };
    }

    @Parameterized.Parameter
    public File strategy;

    @Test
    public void loadStrategy() throws InvestmentStrategyParseException {
        final Optional<InvestmentStrategy> inv = InvestmentStrategy.load(this.strategy);
        Assertions.assertThat(inv).isPresent();
    }

}
