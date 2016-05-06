/*
 * Copyright 2016 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.petrovicky.zonkybot.app;

import java.io.File;
import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.petrovicky.zonkybot.remote.Rating;
import net.petrovicky.zonkybot.strategy.InvestmentStrategy;
import net.petrovicky.zonkybot.strategy.StrategyBuilder;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyParser {

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

    public static InvestmentStrategy parse(final File strategyFile) throws ConfigurationException {
        // read config file
        final PropertiesBuilderParameters props = new Parameters().properties().setFile(strategyFile);
        final FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class);
        builder.configure(props);
        final ImmutableConfiguration config = builder.getConfiguration();
        // prepare strategy
        final StrategyBuilder strategies = new StrategyBuilder();
        BigDecimal sumShares = BigDecimal.ZERO;
        for (final Rating rating : Rating.values()) {
            final boolean preferLongerTerms = StrategyParser.getValue(config, rating, "preferLongerTerms", config::getBoolean);
            final BigDecimal targetShare = StrategyParser.getValue(config, rating, "targetShare", config::getBigDecimal);
            if (targetShare.compareTo(BigDecimal.ZERO) < 0 || targetShare.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalStateException("Target share for rating " + rating + " outside of range <0, 1>: " + targetShare);
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
                throw new IllegalStateException("Maximum acceptable term for rating " + rating + " is smaller than the minimum.");
            }
            final int minAmount = StrategyParser.getValue(config, rating, "minimumAmount", config::getInteger, 0);
            if (minAmount < 200) {
                throw new IllegalStateException("Minimum investment amount for rating " + rating + "  must not be less than 200.");
            }
            final int maxAmount = StrategyParser.getValue(config, rating, "maximumAmount", config::getInteger, 0);
            if (maxAmount < minAmount) {
                throw new IllegalStateException("Maximum investment amount for rating " + rating + "  is smaller than minimum.");
            }
            // TODO should we limit maxAmount for safety?
            strategies.addIndividualStrategy(rating, targetShare, minTerm, maxTerm, minAmount, maxAmount,
                    preferLongerTerms);
        }
        if (sumShares.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException("Sum of target shares is larger than 1: " + sumShares);
        } else if (sumShares.compareTo(BigDecimal.ONE) < 0) {
            StrategyParser.LOGGER.warn("Sum of target shares ({}) does not equal 1.", sumShares);
        }
        return strategies.build();
    }

}
