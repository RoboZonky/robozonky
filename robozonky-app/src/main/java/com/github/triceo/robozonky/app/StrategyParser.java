/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.robozonky.app;

import java.io.File;
import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.triceo.robozonky.Operations;
import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import com.github.triceo.robozonky.strategy.StrategyBuilder;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyParser.class);

    private static <T> T getValue(final ImmutableConfiguration config, final Rating r, final String property, final Function<String, T> supplier) {
        final String propertyName = property + '.' + r.name();
        final String fallbackPropertyName = property + ".default";
        if (config.containsKey(propertyName)) {
            return supplier.apply(propertyName);
        } else if (config.containsKey(fallbackPropertyName)) {
            return supplier.apply(fallbackPropertyName);
        } else {
            throw new IllegalStateException("Investment strategy is incomplete. Missing value for '" + property + "' and rating '" + r + '\'');
        }
    }

    private static <T> T getValue(final ImmutableConfiguration config, final Rating r, final String property, final BiFunction<String, T, T> supplier, final T def) {
        return StrategyParser.getValue(config, r, property, (a) -> supplier.apply(a, def));
    }

    private static ImmutableConfiguration getConfig(final File strategyFile) throws ConfigurationException {
        // read config file
        final PropertiesBuilderParameters props = new Parameters().properties().setFile(strategyFile);
        final FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class);
        builder.configure(props);
        return builder.getConfiguration();
    }

    public static InvestmentStrategy parse(final File strategyFile) throws ConfigurationException {
        final ImmutableConfiguration config = StrategyParser.getConfig(strategyFile);
        final StrategyBuilder strategies = new StrategyBuilder();
        BigDecimal sumShares = BigDecimal.ZERO;
        for (final Rating rating : Rating.values()) { // prepare strategy for a given rating
            final boolean preferLongerTerms =
                    StrategyParser.getValue(config, rating, "preferLongerTerms", config::getBoolean);
            final BigDecimal targetShare =
                    StrategyParser.getValue(config, rating, "targetShare", config::getBigDecimal);
            if (targetShare.compareTo(BigDecimal.ZERO) < 0 || targetShare.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalStateException("Target share for rating " + rating + " outside of range <0, 1>: "
                        + targetShare);
            }
            sumShares = sumShares.add(targetShare);
            final int minTerm = StrategyParser.getValue(config, rating, "minimumTerm", config::getInteger, 0);
            if (minTerm < -1) {
                throw new IllegalStateException("Minimum acceptable term for rating " + rating + " is negative.");
            }
            final int maxTerm = StrategyParser.getValue(config, rating, "maximumTerm", config::getInteger, 0);
            if (maxTerm < -1) {
                throw new IllegalStateException("Maximum acceptable term for rating " + rating + " is negative.");
            } else if (minTerm > maxTerm && maxTerm != -1) {
                throw new IllegalStateException("Maximum acceptable term for rating " + rating
                        + " is smaller than the minimum.");
            }
            final int maxLoanAmount =
                    StrategyParser.getValue(config, rating, "maximumLoanAmount", config::getInteger, 0);
            if (maxLoanAmount < Operations.MINIMAL_INVESTMENT_ALLOWED) {
                throw new IllegalStateException("Maximum investment amount for rating " + rating
                        + "  is smaller than minimum.");
            }
            final BigDecimal maxLoanShare =
                    StrategyParser.getValue(config, rating, "maximumLoanShare", config::getBigDecimal);
            if (maxLoanShare.compareTo(BigDecimal.ZERO) < 0 || maxLoanShare.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalStateException("Maximum investment share for rating " + rating
                        + " outside of range <0, 1>: " + targetShare);
            }
            strategies.addIndividualStrategy(rating, targetShare, minTerm, maxTerm, maxLoanAmount, maxLoanShare,
                    preferLongerTerms);
        }
        if (sumShares.compareTo(BigDecimal.ONE) > 0) {
            StrategyParser.LOGGER.warn("Sum of target shares ({}) is larger than 1. Some ratings are likely to be " +
                    "over-represented.", sumShares);
        } else if (sumShares.compareTo(BigDecimal.ONE) < 0) {
            StrategyParser.LOGGER.warn("Sum of target shares ({}) is smaller than 1. You are likely to leave money " +
                    "unspent in your wallet.", sumShares);
        }
        return strategies.build();
    }

}
