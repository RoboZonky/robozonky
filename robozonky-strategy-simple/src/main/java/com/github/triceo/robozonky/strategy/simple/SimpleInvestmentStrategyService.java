/*
 * Copyright 2017 Lukáš Petrovický
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

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategyService;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleInvestmentStrategyService implements InvestmentStrategyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleInvestmentStrategyService.class);

    private static ImmutableConfiguration getConfig(final InputStream strategy) {
        return ImmutableConfiguration.from(strategy);
    }

    static int getMinimumBalance(final ImmutableConfiguration config) {
        final int minimumBalance = StrategyFileProperty.MINIMUM_BALANCE.getValue(config::getInt)
                .orElseThrow(() -> new IllegalStateException("Minimum balance is missing."));
        if (minimumBalance < Defaults.MINIMUM_INVESTMENT_IN_CZK) {
            throw new IllegalStateException("Minimum balance < " + Defaults.MINIMUM_INVESTMENT_IN_CZK + " CZK.");
        }
        return minimumBalance;
    }

    static int getMaximumInvestment(final ImmutableConfiguration config) {
        final int maximumInvestment = StrategyFileProperty.MAXIMUM_INVESTMENT.getValue(config::getInt)
                .orElseThrow(() -> new IllegalStateException("Maximum investment is missing."));
        if (maximumInvestment < Defaults.MINIMUM_INVESTMENT_IN_CZK) {
            throw new IllegalStateException("Maximum investment < " + Defaults.MINIMUM_INVESTMENT_IN_CZK + " CZK.");
        }
        return maximumInvestment;
    }

    private static StrategyPerRating createIndividualStrategy(final Rating r, final BigDecimal targetShare,
                                                              final BigDecimal maxShare, final int minTerm,
                                                              final int maxTerm, final int minAskAmount,
                                                              final int maxAskAmount, final int minLoanAmount,
                                                              final int maxLoanAmount, final BigDecimal minLoanShare,
                                                              final BigDecimal maxLoanShare,
                                                              final boolean preferLongerTerms,
                                                              final boolean requireConfirmation) {
        if (Objects.equals(targetShare, BigDecimal.ZERO) && Objects.equals(maxShare, BigDecimal.ZERO)) {
            SimpleInvestmentStrategyService.LOGGER.debug("Rating '{}' will never be invested into.", r.getCode());
        } else {
            SimpleInvestmentStrategyService.LOGGER.debug("Acceptable shares of rating '{}' on total investments is " +
                    "between {} and {}.", r.getCode(), targetShare, maxShare);
            SimpleInvestmentStrategyService.LOGGER.debug("Acceptable marketplace for rating '{}' are between {} and {} CZK on " +
                            "terms between {} and {} months, {} preferred.", r.getCode(), minAskAmount,
                    maxAskAmount < 0 ? "+inf" : maxAskAmount, minTerm == -1 ? 0 : minTerm,
                    maxTerm < 0 ? "+inf" : maxTerm + 1, preferLongerTerms ? "longer" : "shorter");
            SimpleInvestmentStrategyService.LOGGER.debug("Acceptable investment for a loan of rating '{}' is between {} " +
                            "and {} CZK with investment share between {} and {}.", r.getCode(), minLoanAmount,
                    maxLoanAmount < 0 ? "+inf" : maxLoanAmount, minLoanShare, maxLoanShare);
            if (requireConfirmation) {
                SimpleInvestmentStrategyService.LOGGER.debug("Rating '{}' requires confirmation.", r.getCode());
            }
        }
        return new StrategyPerRating(r, targetShare, maxShare, minTerm, maxTerm, minLoanAmount, maxLoanAmount,
                minLoanShare, maxLoanShare, minAskAmount, maxAskAmount, preferLongerTerms, requireConfirmation);
    }

    private static StrategyPerRating parseRating(final Rating rating, final ImmutableConfiguration config) {
        SimpleInvestmentStrategyService.LOGGER.debug("- Adding strategy for rating '{}'.", rating.getCode());
        final boolean requireApproval = StrategyFileProperty.REQUIRE_CONFIRMATION.getValue(rating, config::getBoolean);
        final boolean preferLongerTerms = StrategyFileProperty.PREFER_LONGER_TERMS.getValue(rating, config::getBoolean);
        // shares of a rating on the total pie
        final BigDecimal targetShare = StrategyFileProperty.TARGET_SHARE.getValue(rating, config::getBigDecimal);
        if (!Util.isBetweenZeroAndOne(targetShare)) {
            throw new IllegalStateException("Target share for rating " + rating + " outside of range <0, 1>: "
                    + targetShare);
        }
        final BigDecimal maximumShare = StrategyFileProperty.MAXIMUM_SHARE.getValue(rating, config::getBigDecimal);
        if (!Util.isBetweenAAndB(maximumShare, targetShare, BigDecimal.ONE)) {
            throw new IllegalStateException("Maximum share for rating " + rating + " outside of range <" + targetShare
                    + ", 1>: " + maximumShare);
        }
        // terms
        final int minTerm = StrategyFileProperty.MINIMUM_TERM.getValue(rating, config::getInt);
        if (minTerm < -1) {
            throw new IllegalStateException("Minimum acceptable term for rating " + rating + " negative.");
        }
        final int maxTerm = StrategyFileProperty.MAXIMUM_TERM.getValue(rating, config::getInt);
        if (maxTerm < -1) {
            throw new IllegalStateException("Maximum acceptable term for rating " + rating + " negative.");
        } else if (minTerm > maxTerm && maxTerm != -1) {
            throw new IllegalStateException("Maximum acceptable term for rating " + rating + " less than minimum.");
        }
        // loan asks
        final int minAskAmount = StrategyFileProperty.MINIMUM_ASK.getValue(rating, config::getInt);
        if (minAskAmount < -1) {
            throw new IllegalStateException("Minimum acceptable ask amount for rating " + rating + " negative.");
        }
        final int maxAskAmount = StrategyFileProperty.MAXIMUM_ASK.getValue(rating, config::getInt);
        if (maxAskAmount < -1) {
            throw new IllegalStateException("Maximum acceptable ask for rating " + rating + " negative.");
        } else if (minAskAmount > maxAskAmount && maxAskAmount != -1) {
            throw new IllegalStateException("Maximum acceptable ask for rating " + rating + " less than minimum.");
        }
        // investment amounts
        final int minLoanAmount = StrategyFileProperty.MINIMUM_LOAN_AMOUNT.getValue(rating, config::getInt);
        if (minLoanAmount < Defaults.MINIMUM_INVESTMENT_IN_CZK) {
            throw new IllegalStateException("Minimum investment amount for rating " + rating
                    + " less than minimum investment allowed: " + minLoanAmount + " CZK.");
        }
        final int maxLoanAmount = StrategyFileProperty.MAXIMUM_LOAN_AMOUNT.getValue(rating, config::getInt);
        if (maxLoanAmount != -1 && maxLoanAmount < minLoanAmount) {
            throw new IllegalStateException("Maximum investment amount for rating " + rating + " less than minimum.");
        }
        final BigDecimal minLoanShare = StrategyFileProperty.MINIMUM_LOAN_SHARE.getValue(rating,
                config::getBigDecimal);
        if (!Util.isBetweenZeroAndOne(minLoanShare)) {
            throw new IllegalStateException("Minimum investment share for rating " + rating
                    + " outside of range <0, 1>: " + targetShare);
        }
        final BigDecimal maxLoanShare = StrategyFileProperty.MAXIMUM_LOAN_SHARE.getValue(rating, config::getBigDecimal);
        if (!Util.isBetweenAAndB(maxLoanShare, minLoanShare, BigDecimal.ONE)) {
            throw new IllegalStateException("Maximum investment share for rating " + rating
                    + " outside of range (min, 1>: " + targetShare);
        }
        return SimpleInvestmentStrategyService.createIndividualStrategy(rating, targetShare, maximumShare,
                minTerm, maxTerm, minAskAmount, maxAskAmount, minLoanAmount, maxLoanAmount, minLoanShare,
                maxLoanShare, preferLongerTerms, requireApproval);
    }

    private static void checkIndividualStrategies(final Map<Rating, StrategyPerRating> strategies) {
        final BigDecimal ratingShareSum = strategies.values().stream()
                .map(StrategyPerRating::getTargetShare)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (ratingShareSum.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException("Sum total of target shares is larger than 1.00: " + ratingShareSum + ".");
        }
    }

    @Override
    public Optional<InvestmentStrategy> parse(final InputStream strategy)  {
        try {
            final ImmutableConfiguration c = SimpleInvestmentStrategyService.getConfig(strategy);
            final int minimumBalance = SimpleInvestmentStrategyService.getMinimumBalance(c);
            SimpleInvestmentStrategyService.LOGGER.debug("Minimum balance to invest must be {} CZK.", minimumBalance);
            final int maximumInvestment = SimpleInvestmentStrategyService.getMaximumInvestment(c);
            SimpleInvestmentStrategyService.LOGGER.debug("Maximum investment must not exceed {} CZK.", maximumInvestment);
            final Map<Rating, StrategyPerRating> individualStrategies = Arrays.stream(Rating.values())
                    .collect(Collectors.toMap(Function.identity(), r -> SimpleInvestmentStrategyService.parseRating(r, c)));
            SimpleInvestmentStrategyService.checkIndividualStrategies(individualStrategies);
            return Optional.of(new SimpleInvestmentStrategy(minimumBalance, maximumInvestment, individualStrategies));
        } catch (final RuntimeException ex) {
            SimpleInvestmentStrategyService.LOGGER.info("Failed parsing strategy.", ex);
            return Optional.empty();
        }
    }

}
