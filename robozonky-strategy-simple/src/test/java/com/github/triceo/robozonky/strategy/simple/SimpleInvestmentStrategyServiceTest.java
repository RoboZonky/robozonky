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

import com.github.triceo.robozonky.strategy.InvestmentStrategyParseException;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class SimpleInvestmentStrategyServiceTest {

    private static final File PROPER =
            new File("src/test/resources/com/github/triceo/robozonky/strategy/simple/strategy-sample.cfg");
    private static final File IMPROPER =
            new File("src/test/resources/com/github/triceo/robozonky/strategy/simple/strategy-sample.badext");
    private static final File NONEXISTENT =
            new File("src/test/resources/com/github/triceo/robozonky/strategy/simple/nonexistent.cfg");

    @Test
    public void proper() throws InvestmentStrategyParseException {
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        Assertions.assertThat(s.isSupported(SimpleInvestmentStrategyServiceTest.PROPER)).isTrue();
        Assertions.assertThat(s.parse(SimpleInvestmentStrategyServiceTest.PROPER)).isNotNull();
    }

    @Test(expected = InvestmentStrategyParseException.class)
    public void nonexistent() throws InvestmentStrategyParseException {
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        Assertions.assertThat(s.isSupported(SimpleInvestmentStrategyServiceTest.NONEXISTENT)).isTrue();
        s.parse(SimpleInvestmentStrategyServiceTest.NONEXISTENT);
    }

    @Test
    public void improper() throws InvestmentStrategyParseException {
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        Assertions.assertThat(s.isSupported(SimpleInvestmentStrategyServiceTest.IMPROPER)).isFalse();
    }

}
