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

package com.github.triceo.robozonky.strategy;

import java.io.File;
import java.util.Optional;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements Java's {@link ServiceLoader} to provide suitable {@link InvestmentStrategy} implementations.
 */
class InvestmentStrategyLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvestmentStrategyLoader.class);
    private static final ServiceLoader<InvestmentStrategyService> STRATEGY_LOADER =
            ServiceLoader.load(InvestmentStrategyService.class);

    static Optional<InvestmentStrategy> load(final File file) throws InvestmentStrategyParseException {
        if (file == null) {
            throw new NullPointerException("Strategy file can not be null.");
        } else if (!file.canRead()) {
            throw new InvestmentStrategyParseException("Strategy file not accessible: " + file.getAbsolutePath());
        }
        InvestmentStrategyLoader.LOGGER.trace("Loading strategies.");
        try {
            for (final InvestmentStrategyService s : InvestmentStrategyLoader.STRATEGY_LOADER) {
                if (s.isSupported(file)) {
                    InvestmentStrategyLoader.LOGGER.debug("Strategy '{}' will be processed using '{}'.", file.getAbsolutePath(),
                            s.getClass());
                    return Optional.of(s.parse(file));
                } else {
                    InvestmentStrategyLoader.LOGGER.trace("Strategy '{}' will not be processed using '{}'.",
                            file.getAbsolutePath(), s.getClass());
                }
            }
            InvestmentStrategyLoader.LOGGER.warn("No strategy implementation found for '{}'.", file.getAbsolutePath());
            return Optional.empty();
        } finally {
            InvestmentStrategyLoader.LOGGER.trace("Finished loading strategies.");
        }
    }


}

