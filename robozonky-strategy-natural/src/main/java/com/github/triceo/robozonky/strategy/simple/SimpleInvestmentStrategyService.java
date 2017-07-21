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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategyService;
import com.github.triceo.robozonky.strategy.natural.DefaultInvestmentShare;
import com.github.triceo.robozonky.strategy.natural.DefaultPortfolio;
import com.github.triceo.robozonky.strategy.natural.DefaultValues;
import com.github.triceo.robozonky.strategy.natural.InvestmentSize;
import com.github.triceo.robozonky.strategy.natural.LoanAmountCondition;
import com.github.triceo.robozonky.strategy.natural.LoanRatingEnumeratedCondition;
import com.github.triceo.robozonky.strategy.natural.LoanTermCondition;
import com.github.triceo.robozonky.strategy.natural.MarketplaceFilter;
import com.github.triceo.robozonky.strategy.natural.NaturalLanguageInvestmentStrategy;
import com.github.triceo.robozonky.strategy.natural.ParsedStrategy;
import com.github.triceo.robozonky.strategy.natural.PortfolioShare;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class SimpleInvestmentStrategyService implements InvestmentStrategyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleInvestmentStrategyService.class);

    private static int getMinimumBalance(final ImmutableConfiguration config) {
        return StrategyFileProperty.MINIMUM_BALANCE.getValue(config::getInt)
                .orElseThrow(() -> new IllegalStateException("Minimum balance is missing."));
    }

    private static int getTargetPortfolioSize(final ImmutableConfiguration config) {
        return StrategyFileProperty.MAXIMUM_INVESTMENT.getValue(config::getInt)
                .orElseThrow(() -> new IllegalStateException("Maximum investment is missing."));
    }

    private static int getInvestmentShare(final ImmutableConfiguration config) {
        return Stream.of(Rating.values())
                .mapToInt(r ->
                                  SimpleInvestmentStrategyService.getShare(config,
                                                                           StrategyFileProperty.MAXIMUM_LOAN_SHARE, r))
                .distinct()
                .min() // natural strategy only allows for one such value; let's pick the smallest available for safety
                .orElseThrow(() -> new IllegalStateException("Maximum loan share is missing"));
    }

    private static int getShare(final ImmutableConfiguration config, final StrategyFileProperty prop,
                                final Rating rating) {
        final BigDecimal share = prop.getValue(rating, config::getBigDecimal);
        return share.multiply(BigDecimal.valueOf(100)).intValue();
    }

    private static InvestmentStrategy parseOrThrow(final InputStream strategy) {
        final ImmutableConfiguration config = ImmutableConfiguration.from(strategy);
        // set default values
        final DefaultValues d = new DefaultValues(DefaultPortfolio.EMPTY);
        d.setMinimumBalance(SimpleInvestmentStrategyService.getMinimumBalance(config));
        d.setTargetPortfolioSize(SimpleInvestmentStrategyService.getTargetPortfolioSize(config));
        d.setInvestmentShare(new DefaultInvestmentShare(SimpleInvestmentStrategyService.getInvestmentShare(config)));
        final LoanRatingEnumeratedCondition c = new LoanRatingEnumeratedCondition();
        Stream.of(Rating.values())
                .filter(r -> StrategyFileProperty.REQUIRE_CONFIRMATION.getValue(r, config::getBoolean))
                .forEach(c::add);
        d.setConfirmationCondition(c);
        // assemble strategy
        final Collection<InvestmentSize> investmentSizes = Stream.of(Rating.values())
                .map(r -> {
                    final int min = StrategyFileProperty.MINIMUM_LOAN_AMOUNT.getValue(r, config::getInt);
                    final int max = StrategyFileProperty.MAXIMUM_LOAN_AMOUNT.getValue(r, config::getInt);
                    return new InvestmentSize(r, min, max);
                }).collect(Collectors.toList());
        final Collection<PortfolioShare> portfolio = Stream.of(Rating.values())
                .map(r -> {
                    final int min =
                            SimpleInvestmentStrategyService.getShare(config, StrategyFileProperty.TARGET_SHARE, r);
                    final int max =
                            SimpleInvestmentStrategyService.getShare(config, StrategyFileProperty.MAXIMUM_SHARE, r);
                    return new PortfolioShare(r, min, max);
                }).collect(Collectors.toList());
        final Collection<MarketplaceFilter> filters = new ArrayList<>();
        Stream.of(Rating.values()) // filter loans with less than minimum term
                .map(r -> {
                    final int min = StrategyFileProperty.MINIMUM_TERM.getValue(r, config::getInt);
                    return new LoanTermCondition(0, Math.max(0, min - 1)); // <0; 0> is not a problem
                }).map(condition -> {
            final MarketplaceFilter f = new MarketplaceFilter();
            f.ignoreWhen(Collections.singleton(condition));
            return f;
        }).forEach(filters::add);
        Stream.of(Rating.values()) // filter loans with more than maximum term
                .map(r -> {
                    final int max = StrategyFileProperty.MAXIMUM_TERM.getValue(r, config::getInt);
                    return new LoanTermCondition(max + 1);
                }).map(condition -> {
            final MarketplaceFilter f = new MarketplaceFilter();
            f.ignoreWhen(Collections.singleton(condition));
            return f;
        }).forEach(filters::add);
        Stream.of(Rating.values()) // filter loans with ask for less than minimum
                .map(r -> {
                    final int min = StrategyFileProperty.MINIMUM_ASK.getValue(r, config::getInt);
                    return new LoanAmountCondition(0, Math.max(0, min - 1)); // <0; 0> is not a problem
                }).map(condition -> {
            final MarketplaceFilter f = new MarketplaceFilter();
            f.ignoreWhen(Collections.singleton(condition));
            return f;
        }).forEach(filters::add);
        Stream.of(Rating.values()) // filter loans with ask more than maximum
                .map(r -> {
                    final int max = StrategyFileProperty.MAXIMUM_ASK.getValue(r, config::getInt);
                    return new LoanAmountCondition(max + 1);
                }).map(condition -> {
            final MarketplaceFilter f = new MarketplaceFilter();
            f.ignoreWhen(Collections.singleton(condition));
            return f;
        }).forEach(filters::add);
        final ParsedStrategy p = new ParsedStrategy(d, portfolio, investmentSizes, filters);
        return new NaturalLanguageInvestmentStrategy(p);
    }

    @Override
    public Optional<InvestmentStrategy> parse(final InputStream strategy) {
        try {
            final InvestmentStrategy s = SimpleInvestmentStrategyService.parseOrThrow(strategy);
            SimpleInvestmentStrategyService.LOGGER.warn("You are using a deprecated strategy format!");
            return Optional.of(s);
        } catch (final Exception ex) {
            SimpleInvestmentStrategyService.LOGGER.debug("Failed converting to a natural strategy. May be OK.", ex);
            return Optional.empty();
        }
    }
}
