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

package com.github.triceo.robozonky.strategy.rules;

import java.io.File;

import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import com.github.triceo.robozonky.strategy.InvestmentStrategyParseException;
import com.github.triceo.robozonky.strategy.InvestmentStrategyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuleBasedInvestmentStrategyService implements InvestmentStrategyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBasedInvestmentStrategyService.class);

    @Override
    public InvestmentStrategy parse(final File strategyFile) throws InvestmentStrategyParseException {
        return null;
    }

    @Override
    public boolean isSupported(final File strategyFile) {
        return strategyFile.getAbsolutePath().endsWith(".xls") || strategyFile.getAbsolutePath().endsWith(".xlsx");
    }

}
