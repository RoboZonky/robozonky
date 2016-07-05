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
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import com.github.triceo.robozonky.strategy.InvestmentStrategyParseException;
import com.github.triceo.robozonky.strategy.InvestmentStrategyService;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleInvestmentStrategyService implements InvestmentStrategyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleInvestmentStrategyService.class);

    private static <T> T getValue(final ImmutableConfiguration config, final Rating r, final String property,
                                  final Function<String, T> supplier) {
        final String propertyName = property + '.' + r.name();
        final String fallbackPropertyName = property + ".default";
        if (config.containsKey(propertyName)) {
            return supplier.apply(propertyName);
        } else if (config.containsKey(fallbackPropertyName)) {
            return supplier.apply(fallbackPropertyName);
        } else {
            throw new IllegalStateException("Investment strategy is incomplete. Missing value for '" + property
                    + "' and rating '" + r + '\'');
        }
    }

    private static <T> T getValue(final ImmutableConfiguration config, final Rating r, final String property,
                                  final BiFunction<String, T, T> supplier, final T def) {
        return SimpleInvestmentStrategyService.getValue(config, r, property, (a) -> supplier.apply(a, def));
    }

    private static ImmutableConfiguration getConfig(final File strategyFile) throws InvestmentStrategyParseException {
        try {
            return new Configurations().properties(strategyFile);
        } catch (final ConfigurationException ex) {
            throw new InvestmentStrategyParseException("Failed parsing strategy file.", ex);
        }
    }

    @Override
    public InvestmentStrategy parse(final File strategyFile) throws InvestmentStrategyParseException {
        SimpleInvestmentStrategyService.LOGGER.info("Using strategy: '{}'", strategyFile.getAbsolutePath());
        final ImmutableConfiguration config = SimpleInvestmentStrategyService.getConfig(strategyFile);
        final Map<Rating, StrategyPerRating> individualStrategies = new EnumMap<>(Rating.class);
        BigDecimal sumShares = BigDecimal.ZERO;
        for (final Rating rating : Rating.values()) { // prepare strategy for a given rating
            final boolean preferLongerTerms =
                    SimpleInvestmentStrategyService.getValue(config, rating, "preferLongerTerms", config::getBoolean);
            final BigDecimal targetShare =
                    SimpleInvestmentStrategyService.getValue(config, rating, "targetShare", config::getBigDecimal);
            if (targetShare.compareTo(BigDecimal.ZERO) < 0 || targetShare.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalStateException("Target share for rating " + rating + " outside of range <0, 1>: "
                        + targetShare);
            }
            sumShares = sumShares.add(targetShare);
            // terms
            final int minTerm = SimpleInvestmentStrategyService.getValue(config, rating, "minimumTerm",
                    config::getInteger, 0);
            if (minTerm < -1) {
                throw new IllegalStateException("Minimum acceptable term for rating " + rating + " is negative.");
            }
            final int maxTerm = SimpleInvestmentStrategyService.getValue(config, rating, "maximumTerm",
                    config::getInteger, 0);
            if (maxTerm < -1) {
                throw new IllegalStateException("Maximum acceptable term for rating " + rating + " is negative.");
            } else if (minTerm > maxTerm && maxTerm != -1) {
                throw new IllegalStateException("Maximum acceptable term for rating " + rating
                        + " is smaller than the minimum.");
            }
            // loan asks
            final int minAskAmount = SimpleInvestmentStrategyService.getValue(config, rating, "minimumAsk",
                    config::getInteger, 0);
            if (minAskAmount < -1) {
                throw new IllegalStateException("Minimum acceptable ask amount for rating " + rating + " is negative.");
            }
            final int maxAskAmount = SimpleInvestmentStrategyService.getValue(config, rating, "maximumAsk",
                    config::getInteger, 0);
            if (maxAskAmount < -1) {
                throw new IllegalStateException("Maximum acceptable ask for rating " + rating + " is negative.");
            } else if (minAskAmount > maxAskAmount && maxAskAmount != -1) {
                throw new IllegalStateException("Maximum acceptable ask for rating " + rating
                        + " is smaller than the minimum.");
            }
            // investment amounts
            final int maxLoanAmount = SimpleInvestmentStrategyService.getValue(config, rating, "maximumLoanAmount",
                    config::getInteger, 0);
            if (maxLoanAmount < InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED) {
                throw new IllegalStateException("Maximum investment amount for rating " + rating
                        + "  is smaller than minimum.");
            }
            final BigDecimal maxLoanShare = SimpleInvestmentStrategyService.getValue(config, rating, "maximumLoanShare",
                    config::getBigDecimal);
            if (maxLoanShare.compareTo(BigDecimal.ZERO) < 0 || maxLoanShare.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalStateException("Maximum investment share for rating " + rating
                        + " outside of range <0, 1>: " + targetShare);
            }
            individualStrategies.put(rating, SimpleInvestmentStrategyService.createIndividualStrategy(rating,
                    targetShare, minTerm, maxTerm, minAskAmount, maxAskAmount, maxLoanAmount, maxLoanShare,
                    preferLongerTerms));
        }
        if (sumShares.compareTo(BigDecimal.ONE) > 0) {
            SimpleInvestmentStrategyService.LOGGER.warn("Sum of target shares ({}) is larger than 1. Some ratings are "
                    + "likely to be over-represented.", sumShares);
        } else if (sumShares.compareTo(BigDecimal.ONE) < 0) {
            SimpleInvestmentStrategyService.LOGGER.warn("Sum of target shares ({}) is smaller than 1. You are likely "
                    + "to leave money unspent in your wallet.", sumShares);
        }
        return new SimpleInvestmentStrategy(individualStrategies);
    }

    private static StrategyPerRating createIndividualStrategy(final Rating r, final BigDecimal targetShare,
                                                              final int minTerm, final int maxTerm,
                                                              final int minAskAmount, final int maxAskAmount,
                                                              final int maxLoanAmount, final BigDecimal maxLoanShare,
                                                              final boolean preferLongerTerms) {
        SimpleInvestmentStrategyService.LOGGER.debug("Adding strategy for rating '{}'.", r.getCode());
        SimpleInvestmentStrategyService.LOGGER.debug("Target share for rating '{}' among total investments is {}.", r.getCode(),
                targetShare);
        SimpleInvestmentStrategyService.LOGGER.debug("Range of acceptable loan amounts for rating '{}' is <{}, {}> CZK.",
                r.getCode(), minAskAmount, maxAskAmount < 0 ? "+inf" : maxAskAmount);
        SimpleInvestmentStrategyService.LOGGER.debug("Range of acceptable investment terms for rating '{}' is <{}, {}) months.",
                r.getCode(), minTerm == -1 ? 0 : minTerm, maxTerm < 0 ? "+inf" : maxTerm + 1);
        SimpleInvestmentStrategyService.LOGGER.debug("Maximum investment amount for rating '{}' is {} CZK.", r.getCode(), maxLoanAmount);
        SimpleInvestmentStrategyService.LOGGER.debug("Maximum investment share for rating '{}' is {}.", r.getCode(), maxLoanShare);
        if (preferLongerTerms) {
            SimpleInvestmentStrategyService.LOGGER.debug("Rating '{}' will prefer longer terms.", r.getCode());
        } else {
            SimpleInvestmentStrategyService.LOGGER.debug("Rating '{}' will prefer shorter terms.", r.getCode());
        }
        return new StrategyPerRating(r, targetShare, minTerm, maxTerm, maxLoanAmount, maxLoanShare, minAskAmount,
                maxAskAmount, preferLongerTerms);
    }

    @Override
    public boolean isSupported(final File strategyFile) {
        return strategyFile.getAbsolutePath().endsWith(".cfg");
    }

}
