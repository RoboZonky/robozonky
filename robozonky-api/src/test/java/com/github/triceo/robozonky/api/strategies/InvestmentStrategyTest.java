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

import org.junit.Assume;
import org.junit.Test;

public class InvestmentStrategyTest {

    @Test(expected = InvestmentStrategyParseException.class)
    public void nullStrategyAsFile() throws InvestmentStrategyParseException {
        InvestmentStrategyLoader.load((String)null);
    }

    @Test(expected = InvestmentStrategyParseException.class)
    public void nullStrategyAsString() throws InvestmentStrategyParseException {
        InvestmentStrategyLoader.load((File)null);
    }

    @Test(expected = InvestmentStrategyParseException.class)
    public void nonExistentStrategyAsFile() throws InvestmentStrategyParseException, IOException {
        final File strategy = File.createTempFile("robozonky-", ".cfg");
        Assume.assumeTrue(strategy.delete());
        InvestmentStrategyLoader.load(strategy);
    }

    @Test(expected = InvestmentStrategyParseException.class)
    public void nonExistentStrategyAsUrl() throws InvestmentStrategyParseException, IOException {
        final File strategy = File.createTempFile("robozonky-", ".cfg");
        Assume.assumeTrue(strategy.delete());
        InvestmentStrategyLoader.load("file://" + strategy.toPath());
    }
}
