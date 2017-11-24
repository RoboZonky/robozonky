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

package com.github.robozonky.strategy.natural;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParsedStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParsedStrategy.class);

    private final DefaultValues defaults;
    private final Map<Rating, PortfolioShare> portfolio;
    private final Map<Rating, InvestmentSize> investmentSizes;
    private final Collection<MarketplaceFilter> primaryMarketplaceFilters, secondaryMarketplaceFilters, sellFilters;
    private RoboZonkyVersion minimumVersion;

    public ParsedStrategy(final DefaultPortfolio portfolio) {
        this(portfolio, Collections.emptyList());
    }

    ParsedStrategy(final DefaultPortfolio portfolio, final Collection<MarketplaceFilter> primaryMarketplaceFilters) {
        this(portfolio, primaryMarketplaceFilters, Collections.emptyList());
    }

    ParsedStrategy(final DefaultPortfolio portfolio, final Collection<MarketplaceFilter> primaryMarketplaceFilters,
                   final Collection<MarketplaceFilter> secondaryMarketplaceFilters) {
        this(portfolio, primaryMarketplaceFilters, secondaryMarketplaceFilters, Collections.emptyList());
    }

    ParsedStrategy(final DefaultPortfolio portfolio, final Collection<MarketplaceFilter> primaryMarketplaceFilters,
                   final Collection<MarketplaceFilter> secondaryMarketplaceFilters,
                   final Collection<MarketplaceFilter> sellFilters) {
        this(new DefaultValues(portfolio), Collections.emptyList(), Collections.emptyMap(), primaryMarketplaceFilters,
             secondaryMarketplaceFilters, sellFilters);
    }

    public ParsedStrategy(final DefaultValues defaults, final Collection<PortfolioShare> portfolio,
                          final Map<Rating, InvestmentSize> investmentSizes,
                          final Collection<MarketplaceFilter> primaryMarketplaceFilters,
                          final Collection<MarketplaceFilter> secondaryMarketplaceFilters,
                          final Collection<MarketplaceFilter> sellFilters) {
        this.defaults = defaults;
        this.portfolio = portfolio.stream().collect(Collectors.toMap(PortfolioShare::getRating, Function.identity()));
        this.investmentSizes = investmentSizes;
        this.primaryMarketplaceFilters = primaryMarketplaceFilters;
        this.secondaryMarketplaceFilters = secondaryMarketplaceFilters;
        this.sellFilters = sellFilters;
        final int shareSum = sumMinimalShares();
        if (shareSum > 100) {
            throw new IllegalArgumentException("Sum of minimal rating shares in portfolio is over 100 %.");
        } else if (shareSum < 100) {
            ParsedStrategy.LOGGER.info("Sum of minimal rating shares in the portfolio is less than 100 %.");
        }
    }

    private static boolean matchesAnyFilter(final Wrapper item, final Stream<MarketplaceFilter> filters,
                                            final String logMessage) {
        return filters.filter(f -> f.test(item))
                .peek(f -> ParsedStrategy.LOGGER.debug(logMessage, item.getIdentifier(), f))
                .findFirst()
                .isPresent();
    }

    private static boolean matchesNoFilter(final Wrapper item, final Collection<MarketplaceFilter> filters) {
        return !matchesAnyFilter(item, filters.stream(), "{} ignored, matched {}.");
    }

    private int sumMinimalShares() {
        return Stream.of(Rating.values()).mapToInt(this::getMinimumShare).sum();
    }

    public boolean needsConfirmation(final LoanDescriptor loan) {
        return defaults.needsConfirmation(loan.item());
    }

    public int getMinimumBalance() {
        return defaults.getMinimumBalance();
    }

    public int getMinimumInvestmentShareInPercent() {
        return defaults.getInvestmentShare().getMinimumShareInPercent();
    }

    public int getMaximumInvestmentShareInPercent() {
        return defaults.getInvestmentShare().getMaximumShareInPercent();
    }

    public int getMaximumInvestmentSizeInCzk() {
        return defaults.getTargetPortfolioSize();
    }

    public Optional<RoboZonkyVersion> getMinimumVersion() {
        return Optional.ofNullable(minimumVersion);
    }

    public void setMinimumVersion(final RoboZonkyVersion minimumVersion) {
        this.minimumVersion = minimumVersion;
    }

    public int getMinimumShare(final Rating rating) {
        if (portfolio.containsKey(rating)) {
            return portfolio.get(rating).getMininumShareInPercent();
        } else { // no minimum share specified; use the one from default portfolio
            return defaults.getPortfolio().getDefaultShare(rating);
        }
    }

    public int getMaximumShare(final Rating rating) {
        if (portfolio.containsKey(rating)) {
            return portfolio.get(rating).getMaximumShareInPercent();
        } else { // no maximum share specified; calculate minimum share and use it as maximum too
            return this.getMinimumShare(rating);
        }
    }

    private InvestmentSize getInvestmentSize(final Rating rating) {
        return investmentSizes.getOrDefault(rating, defaults.getInvestmentSize());
    }

    public int getMinimumInvestmentSizeInCzk(final Rating rating) {
        return getInvestmentSize(rating).getMinimumInvestmentInCzk();
    }

    public int getMaximumInvestmentSizeInCzk(final Rating rating) {
        return getInvestmentSize(rating).getMaximumInvestmentInCzk();
    }

    private boolean matchesAnySellFilter(final Wrapper item) {
        return matchesAnyFilter(item, sellFilters.stream(), "{} matched {}.");
    }

    public Stream<LoanDescriptor> getApplicableLoans(final Collection<LoanDescriptor> items) {
        return items.stream().filter(i -> {
            final Wrapper w = new Wrapper(i.item());
            return matchesNoFilter(w, primaryMarketplaceFilters);
        });
    }

    public Stream<ParticipationDescriptor> getApplicableParticipations(
            final Collection<ParticipationDescriptor> items) {
        return items.stream().filter(i -> {
            final Wrapper w = new Wrapper(i.item(), i.related());
            return matchesNoFilter(w, secondaryMarketplaceFilters);
        });
    }

    public Stream<InvestmentDescriptor> getApplicableInvestments(final Collection<InvestmentDescriptor> items) {
        return items.stream().filter(i -> {
            final Wrapper w = new Wrapper(i.item(), i.related());
            return matchesAnySellFilter(w);
        });
    }

    @Override
    public String toString() {
        return "ParsedStrategy{" +
                "defaults=" + defaults +
                ", investmentSizes=" + investmentSizes +
                ", minimumVersion=" + minimumVersion +
                ", portfolio=" + portfolio +
                ", primaryMarketplaceFilters=" + primaryMarketplaceFilters +
                ", secondaryMarketplaceFilters=" + secondaryMarketplaceFilters +
                ", sellFilters=" + sellFilters +
                '}';
    }
}
