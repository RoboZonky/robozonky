package net.petrovicky.zonkybot.app;

import java.io.File;
import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.petrovicky.zonkybot.api.remote.Rating;
import net.petrovicky.zonkybot.strategy.Strategy;
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

    private static <T> T getValue(ImmutableConfiguration config, Rating r, String property, Function<String, T> supplier) {
        String propertyName = property + "." + r.name();
        String fallbackPropertyName = property + ".default";
        if (config.containsKey(propertyName)) {
            return supplier.apply(propertyName);
        } else if (config.containsKey(fallbackPropertyName)) {
            return supplier.apply(fallbackPropertyName);
        } else {
            throw new IllegalStateException("Investment strategy is incomplete. Missing value for '" + property + "' and rating '" + r + "'");
        }
    }

    private static <T> T getValue(ImmutableConfiguration config, Rating r, String property, BiFunction<String, T, T> supplier, T def) {
        return getValue(config, r, property, (a) -> supplier.apply(a, def));
    }

    public static Strategy parse(File strategyFile) throws ConfigurationException {
        // read config file
        PropertiesBuilderParameters props = new Parameters().properties().setFile(strategyFile);
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class);
        builder.configure(props);
        ImmutableConfiguration config = builder.getConfiguration();
        // prepare strategy
        StrategyBuilder strategies = new StrategyBuilder();
        BigDecimal sumShares = BigDecimal.ZERO;
        for (Rating rating : Rating.values()) {
            BigDecimal targetShare = getValue(config, rating, "targetShare", config::getBigDecimal);
            if (targetShare.compareTo(BigDecimal.ZERO) < 0 || targetShare.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalStateException("Target share for rating " + rating + " outside of range <0, 1>: " + targetShare);
            }
            sumShares = sumShares.add(targetShare);
            int minTerm = getValue(config, rating, "minimumTerm", config::getInteger, 0);
            if (minTerm < -1) {
                throw new IllegalStateException("Minimum acceptable term for rating " + rating + " is negative.");
            }
            int maxTerm = getValue(config, rating, "maximumTerm", config::getInteger, 0);
            if (maxTerm < -1) {
                throw new IllegalStateException("Maximum acceptable term for rating " + rating + " is negative.");
            } else if (minTerm > maxTerm && maxTerm != -1) {
                throw new IllegalStateException("Maximum acceptable term for rating " + rating + " is smaller than the minimum.");
            }
            int minAmount = getValue(config, rating, "minimumAmount", config::getInteger, 0);
            if (minAmount < 200) {
                throw new IllegalStateException("Minimum investment amount for rating " + rating + "  must not be less than 200.");
            }
            int maxAmount = getValue(config, rating, "maximumAmount", config::getInteger, 0);
            if (maxAmount < minAmount) {
                throw new IllegalStateException("Maximum investment amount for rating " + rating + "  is smaller than minimum.");
            }
            // TODO should we limit maxAmount for safety?
            strategies.addIndividualStrategy(rating, targetShare, minTerm, maxTerm, minAmount, maxAmount);
        }
        if (sumShares.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException("Sum of target shares is larger than 1: " + sumShares);
        } else if (sumShares.compareTo(BigDecimal.ONE) < 0) {
            LOGGER.warn("Sum of target shares ({}) does not equal 1.", sumShares);
        }
        return strategies.build();
    }

}
