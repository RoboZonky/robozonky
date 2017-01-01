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
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Use Java's {@link ServiceLoader} to load different strategy implementations.
 */
public interface InvestmentStrategyService {

    /**
     * Prepare the investment strategy for being used by the app.
     *
     * @param strategyFile Investment strategy in question.
     * @return Processed instance of the investment strategy provided by the user, if the file format is supported.
     * @throws InvestmentStrategyParseException If the strategy file failed to initialize.
     */
    Optional<InvestmentStrategy> parse(File strategyFile) throws InvestmentStrategyParseException;

}
