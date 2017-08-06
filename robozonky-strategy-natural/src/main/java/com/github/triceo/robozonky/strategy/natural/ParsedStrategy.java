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

package com.github.triceo.robozonky.strategy.natural;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Participation;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.ParticipationDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParsedStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParsedStrategy.class);

    private final DefaultValues defaults;
    private final Map<Rating, PortfolioShare> portfolio;
    private final Map<Rating, InvestmentSize> investmentSizes;
    private final Collection<MarketplaceFilter> primaryMarketplaceFilters, secondaryMarketplaceFilters;

    public ParsedStrategy(final DefaultPortfolio portfolio) {
        this(portfolio, Collections.emptyMap());
    }

    ParsedStrategy(final DefaultPortfolio portfolio, final Map<Boolean, Collection<MarketplaceFilter>> filters) {
        this(new DefaultValues(portfolio), Collections.emptyList(), Collections.emptyList(), filters);
    }

    public ParsedStrategy(final DefaultValues defaults,
                          final Collection<PortfolioShare> portfolio,
                          final Collection<InvestmentSize> investmentSizes,
                          final Map<Boolean, Collection<MarketplaceFilter>> marketplaceFilters) {
        this.defaults = defaults;
        this.portfolio = portfolio.stream()
                .collect(Collectors.toMap(PortfolioShare::getRating, Function.identity()));
        this.investmentSizes = investmentSizes.stream()
                .collect(Collectors.toMap(InvestmentSize::getRating, Function.identity()));
        this.primaryMarketplaceFilters = marketplaceFilters.getOrDefault(true, Collections.emptyList());
        this.secondaryMarketplaceFilters = marketplaceFilters.getOrDefault(false, Collections.emptyList());
        final int shareSum = sumMinimalShares();
        if (shareSum > 100) {
            throw new IllegalArgumentException("Sum of minimal rating shares in portfolio is over 100 %.");
        } else if (shareSum < 100) {
            ParsedStrategy.LOGGER.info("Sum of minimal rating shares in the portfolio is less than 100 %.");
        }
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

    public int getMinimumInvestmentSizeInCzk(final Rating rating) {
        if (investmentSizes.containsKey(rating)) {
            return investmentSizes.get(rating).getMinimumInvestmentInCzk();
        } else { // no minimum share specified; use default
            return defaults.getInvestmentSize().getMinimumInvestmentInCzk();
        }
    }

    public int getMaximumInvestmentSizeInCzk(final Rating rating) {
        if (investmentSizes.containsKey(rating)) {
            return investmentSizes.get(rating).getMaximumInvestmentInCzk();
        } else { // no maximum share specified; use default
            return defaults.getInvestmentSize().getMaximumInvestmentInCzk();
        }
    }

    private Collection<MarketplaceFilter> getProperFilters(final Object item) {
        if (item instanceof Loan) {
            return primaryMarketplaceFilters;
        } else if (item instanceof Participation) {
            return secondaryMarketplaceFilters;
        } else {
            throw new IllegalStateException("Wrong object type: " + item.getClass());
        }
    }

    private boolean matchesNoFilter(final Object item, final int loanId) {
        final Collection<MarketplaceFilter> filters = getProperFilters(item);
        return !filters.stream()
                .filter(f -> f.test(item))
                .peek(f -> ParsedStrategy.LOGGER.debug("Loan #{} ignored, matched {}.", loanId, f))
                .findFirst()
                .isPresent();
    }

    public Stream<LoanDescriptor> getApplicableLoans(final Collection<LoanDescriptor> items) {
        return items.stream().filter(i -> matchesNoFilter(i.item(), i.item().getId()));
    }

    public Stream<ParticipationDescriptor> getApplicableParticipations(
            final Collection<ParticipationDescriptor> items) {
        return items.stream().filter(i -> matchesNoFilter(i.item(), i.item().getLoanId()));
    }
}
