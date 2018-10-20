/*
 * Copyright 2018 The RoboZonky Project
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
import java.util.EnumMap;
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

class ParsedStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParsedStrategy.class);

    private final DefaultValues defaults;
    private final Map<Rating, PortfolioShare> portfolio;
    private final Map<Rating, InvestmentSize> investmentSizes;
    private final FilterSupplier filters;
    private RoboZonkyVersion minimumVersion;

    public ParsedStrategy(final DefaultPortfolio portfolio) {
        this(portfolio, Collections.emptySet());
    }

    ParsedStrategy(final DefaultValues values) {
        this(values, Collections.emptySet(), Collections.emptyMap(), new FilterSupplier(values));
    }

    ParsedStrategy(final DefaultPortfolio portfolio, final Collection<MarketplaceFilter> filters) {
        this(new DefaultValues(portfolio), filters);
    }

    ParsedStrategy(final DefaultValues values, final Collection<MarketplaceFilter> filters) {
        this(values, Collections.emptyList(), Collections.emptyMap(), new FilterSupplier(values, filters));
    }

    ParsedStrategy(final DefaultValues defaults, final Collection<PortfolioShare> portfolio,
                   final Map<Rating, InvestmentSize> investmentSizes) {
        this(defaults, portfolio, investmentSizes, new FilterSupplier(defaults));
    }

    public ParsedStrategy(final DefaultValues defaults, final Collection<PortfolioShare> portfolio,
                          final Map<Rating, InvestmentSize> investmentSizes, final FilterSupplier filters) {
        this.defaults = defaults;
        this.portfolio = portfolio.isEmpty() ? Collections.emptyMap() :
                new EnumMap<>(portfolio.stream().
                        collect(Collectors.toMap(PortfolioShare::getRating, Function.identity())));
        this.investmentSizes = investmentSizes.isEmpty() ? Collections.emptyMap() : new EnumMap<>(investmentSizes);
        this.filters = filters;
        final int shareSum = sumMinimalShares();
        if (shareSum > 100) {
            throw new IllegalArgumentException("Sum of minimal rating shares in portfolio is over 100 %.");
        } else if (shareSum < 100) {
            LOGGER.info("Sum of minimal rating shares in the portfolio is less than 100 %.");
        }
    }

    private static boolean matchesFilter(final Wrapper<?> item, final Collection<MarketplaceFilter> filters,
                                         final String logMessage) {
        return filters.stream()
                .filter(f -> f.test(item))
                .peek(f -> Decisions.report(logger -> logger.debug(logMessage, item, f)))
                .findFirst()
                .isPresent();
    }

    private int sumMinimalShares() {
        return Stream.of(Rating.values()).mapToInt(this::getMinimumShare).sum();
    }

    public boolean needsConfirmation(final LoanDescriptor loan) {
        return defaults.needsConfirmation(loan);
    }

    public long getMinimumBalance() {
        return defaults.getMinimumBalance();
    }

    public int getMinimumInvestmentShareInPercent() {
        return defaults.getInvestmentShare().getMinimumShareInPercent();
    }

    public int getMaximumInvestmentShareInPercent() {
        return defaults.getInvestmentShare().getMaximumShareInPercent();
    }

    public long getMaximumInvestmentSizeInCzk() {
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

    public Stream<LoanDescriptor> getApplicableLoans(final Collection<LoanDescriptor> l) {
        if (!isInvestingEnabled()) {
            return Stream.empty();
        }
        return l.parallelStream()
                .map(Wrapper::wrap)
                .filter(w -> !matchesFilter(w, filters.getPrimaryMarketplaceFilters(),
                                            "{} to be ignored as it matched primary marketplace filter {}."))
                .filter(w -> !matchesFilter(w, filters.getSellFilters(),
                                            "{} to be ignored as it matched sell filter {}."))
                .map(Wrapper::getOriginal);
    }

    public Stream<ParticipationDescriptor> getApplicableParticipations(final Collection<ParticipationDescriptor> p) {
        if (!isPurchasingEnabled()) {
            return Stream.empty();
        }
        return p.parallelStream()
                .map(Wrapper::wrap)
                .filter(w -> !matchesFilter(w, filters.getSecondaryMarketplaceFilters(),
                                            "{} to be ignored as it matched secondary marketplace filter {}."))
                .filter(w -> !matchesFilter(w, filters.getSellFilters(),
                                            "{} to be ignored as it matched sell filter {}."))
                .map(Wrapper::getOriginal);
    }

    public boolean isSellingEnabled() {
        return !filters.getSellFilters().isEmpty();
    }

    public boolean isPurchasingEnabled() {
        return filters.isSecondaryMarketplaceEnabled();
    }

    public boolean isInvestingEnabled() {
        return filters.isPrimaryMarketplaceEnabled();
    }

    public Stream<InvestmentDescriptor> getApplicableInvestments(final Collection<InvestmentDescriptor> i) {
        return i.parallelStream()
                .map(Wrapper::wrap)
                .filter(w -> matchesFilter(w, filters.getSellFilters(),
                                           "{} to be sold as it matched sell filter {}."))
                .map(Wrapper::getOriginal);
    }

    @Override
    public String toString() {
        return "ParsedStrategy{" +
                "defaults=" + defaults +
                ", investmentSizes=" + investmentSizes +
                ", minimumVersion=" + minimumVersion +
                ", portfolio=" + portfolio +
                ", filters=" + filters +
                '}';
    }
}
