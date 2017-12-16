/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.strategy.simple;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.api.strategies.StrategyService;
import com.github.robozonky.strategy.natural.DefaultInvestmentShare;
import com.github.robozonky.strategy.natural.DefaultPortfolio;
import com.github.robozonky.strategy.natural.DefaultValues;
import com.github.robozonky.strategy.natural.InvestmentSize;
import com.github.robozonky.strategy.natural.NaturalLanguageInvestmentStrategy;
import com.github.robozonky.strategy.natural.ParsedStrategy;
import com.github.robozonky.strategy.natural.PortfolioShare;
import com.github.robozonky.strategy.natural.conditions.LoanAmountCondition;
import com.github.robozonky.strategy.natural.conditions.LoanRatingEnumeratedCondition;
import com.github.robozonky.strategy.natural.conditions.LoanTermCondition;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilterCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class SimpleStrategyService implements StrategyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleStrategyService.class);

    private static int getMinimumBalance(final ImmutableConfiguration config) {
        return StrategyFileProperty.MINIMUM_BALANCE.getValue(config::getInt)
                .orElseThrow(() -> new IllegalStateException("Minimum balance is missing"));
    }

    private static int getTargetPortfolioSize(final ImmutableConfiguration config) {
        return StrategyFileProperty.MAXIMUM_INVESTMENT.getValue(config::getInt)
                .orElseThrow(() -> new IllegalStateException("Maximum investment is missing"));
    }

    private static int getInvestmentShare(final ImmutableConfiguration config) {
        return Stream.of(Rating.values())
                .mapToInt(r ->
                                  SimpleStrategyService.getShare(config,
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

    private static MarketplaceFilter getRatingFilter(final Rating r, final MarketplaceFilterCondition condition) {
        final LoanRatingEnumeratedCondition c = new LoanRatingEnumeratedCondition();
        c.add(Collections.singleton(r));
        final MarketplaceFilter f = new MarketplaceFilter();
        f.ignoreWhen(Arrays.asList(c, condition));
        return f;
    }

    private static InvestmentStrategy parseOrThrow(final String strategy) {
        final ImmutableConfiguration config = ImmutableConfiguration.from(strategy);
        // set default values
        final DefaultValues d = new DefaultValues(DefaultPortfolio.EMPTY);
        d.setMinimumBalance(SimpleStrategyService.getMinimumBalance(config));
        d.setTargetPortfolioSize(SimpleStrategyService.getTargetPortfolioSize(config));
        d.setInvestmentShare(new DefaultInvestmentShare(SimpleStrategyService.getInvestmentShare(config)));
        final LoanRatingEnumeratedCondition c = new LoanRatingEnumeratedCondition();
        Stream.of(Rating.values())
                .filter(r -> StrategyFileProperty.REQUIRE_CONFIRMATION.getValue(r, config::getBoolean))
                .forEach(c::add);
        d.setConfirmationCondition(c);
        // assemble strategy
        final Map<Rating, InvestmentSize> investmentSizes = Stream.of(Rating.values())
                .collect(Collectors.toMap(Function.identity(), r -> {
                    final int min = StrategyFileProperty.MINIMUM_LOAN_AMOUNT.getValue(r, config::getInt);
                    final int max = StrategyFileProperty.MAXIMUM_LOAN_AMOUNT.getValue(r, config::getInt);
                    return new InvestmentSize(min, max);
                }));
        final Collection<PortfolioShare> portfolio = Stream.of(Rating.values())
                .map(r -> {
                    final int min =
                            SimpleStrategyService.getShare(config, StrategyFileProperty.TARGET_SHARE, r);
                    final int max =
                            SimpleStrategyService.getShare(config, StrategyFileProperty.MAXIMUM_SHARE, r);
                    return new PortfolioShare(r, min, max);
                }).collect(Collectors.toList());
        // filter loans with less than minimum term
        final Stream<Optional<MarketplaceFilter>> s1 = Stream.of(Rating.values())
                .map(r -> {
                    final int min = StrategyFileProperty.MINIMUM_TERM.getValue(r, config::getInt);
                    if (min < 2) { // there will be no loan with term smaller than 1
                        return Optional.empty();
                    }
                    final MarketplaceFilterCondition f = new LoanTermCondition(0, min - 1);
                    return Optional.of(getRatingFilter(r, f));
                });
        // filter loans with more than maximum term
        final Stream<Optional<MarketplaceFilter>> s2 = Stream.of(Rating.values())
                .map(r -> {
                    final int max = StrategyFileProperty.MAXIMUM_TERM.getValue(r, config::getInt);
                    if (max < 1) {
                        return Optional.empty();
                    }
                    final MarketplaceFilterCondition f = new LoanTermCondition(max + 1);
                    return Optional.of(getRatingFilter(r, f));
                });
        // filter loans with ask for less than minimum
        final Stream<Optional<MarketplaceFilter>> s3 = Stream.of(Rating.values())
                .map(r -> {
                    final int min = StrategyFileProperty.MINIMUM_ASK.getValue(r, config::getInt);
                    if (min < 2) { // amount smaller than 1 is 0, no need to have that filter
                        return Optional.empty();
                    }
                    final MarketplaceFilterCondition f = new LoanAmountCondition(0, min - 1);
                    return Optional.of(getRatingFilter(r, f));
                });
        // filter loans with ask more than maximum
        final Stream<Optional<MarketplaceFilter>> s4 = Stream.of(Rating.values())
                .map(r -> {
                    final int max = StrategyFileProperty.MAXIMUM_ASK.getValue(r, config::getInt);
                    if (max < 1) {
                        return Optional.empty();
                    }
                    final MarketplaceFilterCondition f = new LoanAmountCondition(max + 1);
                    return Optional.of(getRatingFilter(r, f));
                });
        // put all filters together
        final Collection<MarketplaceFilter> filters = Stream.of(s1, s2, s3, s4)
                .flatMap(Function.identity())
                .flatMap(s -> s.map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toList());
        // and create the strategy
        final ParsedStrategy p = new ParsedStrategy(d, portfolio, investmentSizes, filters, Collections.emptyList(),
                                                    Collections.emptyList());
        LOGGER.debug("Converted strategy: {}.", p);
        final InvestmentStrategy result = new NaturalLanguageInvestmentStrategy(p);
        return new SimpleStrategyService.ExclusivelyPrimaryMarketplaceInvestmentStrategy(result);
    }

    @Override
    public Optional<InvestmentStrategy> toInvest(final String strategy) {
        try {
            final InvestmentStrategy s = SimpleStrategyService.parseOrThrow(strategy);
            SimpleStrategyService.LOGGER.warn("Using legacy strategy format. Secondary marketplace support disabled.");
            return Optional.of(s);
        } catch (final Exception ex) {
            SimpleStrategyService.LOGGER.debug("Failed parsing strategy, OK if using natural directly: {}.",
                                               ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<SellStrategy> toSell(final String strategy) {
        return Optional.empty(); // not supported
    }

    @Override
    public Optional<PurchaseStrategy> toPurchase(final String strategy) {
        return Optional.empty(); // not supported
    }

    private static final class ExclusivelyPrimaryMarketplaceInvestmentStrategy implements InvestmentStrategy {

        private final InvestmentStrategy child;

        public ExclusivelyPrimaryMarketplaceInvestmentStrategy(final InvestmentStrategy child) {
            this.child = child;
        }

        @Override
        public Stream<RecommendedLoan> recommend(final Collection<LoanDescriptor> availableLoans,
                                                 final PortfolioOverview portfolio, final Restrictions restrictions) {
            return child.recommend(availableLoans, portfolio, restrictions);
        }
    }
}
