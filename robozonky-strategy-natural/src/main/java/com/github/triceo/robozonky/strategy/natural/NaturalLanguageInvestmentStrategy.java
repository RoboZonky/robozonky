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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NaturalLanguageInvestmentStrategy implements InvestmentStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(NaturalLanguageInvestmentStrategy.class);
    private static final Comparator<LoanDescriptor>
            BY_TERM = Comparator.comparingInt(l -> l.getLoan().getTermInMonths()),
            BY_RECENCY = Comparator.comparing((LoanDescriptor l) -> l.getLoan().getDatePublished()).reversed(),
            BY_REMAINING = Comparator.comparing((LoanDescriptor l) -> l.getLoan().getRemainingInvestment()).reversed();

    static Map<Rating, Collection<LoanDescriptor>> sortLoansByRating(final Collection<LoanDescriptor> loans) {
        return Collections.unmodifiableMap(loans.stream().distinct()
                .collect(Collectors.groupingBy(l -> l.getLoan().getRating())));
    }

    private final ParsedStrategy strategy;

    public NaturalLanguageInvestmentStrategy(final ParsedStrategy p) {
        this.strategy = p;
    }

    /**
     * Pick a loan ordering such that it, first, matches the strategy and, second, maximizes the chances the loan is
     * still available on the marketplace when the investment operation is triggered.
     *
     * @return Ordered marketplace.
     */
    private static Comparator<LoanDescriptor> getLoanComparator() {
        return NaturalLanguageInvestmentStrategy.BY_TERM
                .thenComparing(NaturalLanguageInvestmentStrategy.BY_REMAINING)
                .thenComparing(NaturalLanguageInvestmentStrategy.BY_RECENCY);
    }

    private List<Rating> rankRatingsByDemand(final Map<Rating, BigDecimal> currentShare) {
        final SortedMap<BigDecimal, EnumSet<Rating>> mostWantedRatings = new TreeMap<>(Comparator.reverseOrder());
        // put the ratings into buckets based on how much we're missing them
        currentShare.forEach((r, currentRatingShare) -> {
            final BigDecimal maximumAllowedShare = BigDecimal.valueOf(strategy.getMaximumShare(r))
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN);
            final BigDecimal undershare = maximumAllowedShare.subtract(currentRatingShare);
            if (undershare.compareTo(BigDecimal.ZERO) <= 0) { // we over-invested into this rating; do not include
                return;
            }
            mostWantedRatings.compute(undershare, (k, v) -> {
                if (v == null) {
                    return EnumSet.of(r);
                }
                v.add(r);
                return v;
            });
        });
        return mostWantedRatings.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    private boolean isAcceptable(final PortfolioOverview portfolio) {
        final int balance = portfolio.getCzkAvailable();
        if (balance < Defaults.MINIMUM_INVESTMENT_IN_CZK) { // FIXME implement
            NaturalLanguageInvestmentStrategy.LOGGER.debug("{} CZK balance is less than minimum {} CZK. Ending.",
                    balance, Defaults.MINIMUM_INVESTMENT_IN_CZK);
            return false;
        }
        final int invested = portfolio.getCzkInvested();
        final int investmentCeiling = strategy.getMaximumInvestmentSizeInCzk();
        if (invested > investmentCeiling) {
            NaturalLanguageInvestmentStrategy.LOGGER.debug("{} CZK total investment over {} CZK ceiling. Ending.",
                    invested, investmentCeiling);
            return false;
        }
        return true;
    }

    private boolean needsConfirmation(final LoanDescriptor loanDescriptor) {
        return strategy.needsConfirmation(loanDescriptor);
    }

    @Override
    public List<Recommendation> recommend(final Collection<LoanDescriptor> loans, final PortfolioOverview portfolio) {
        if (!this.isAcceptable(portfolio)) {
            return Collections.emptyList();
        }
        final Collection<LoanDescriptor> availableLoans = strategy.getApplicableLoans(loans)
                .collect(Collectors.toList());
        // split available marketplace into buckets per rating
        final Map<Rating, Collection<LoanDescriptor>> splitByRating =
                NaturalLanguageInvestmentStrategy.sortLoansByRating(availableLoans);
        // prepare map of ratings and their shares; we ignore ratings that have no marketplace available
        final Map<Rating, BigDecimal> relevantPortfolio = splitByRating.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), portfolio::getShareOnInvestment));
        final List<Recommendation> recommendations = this.rankRatingsByDemand(relevantPortfolio).stream()
                .flatMap(rating -> { // prioritize marketplace by their ranking's demand
                    final Comparator<LoanDescriptor> comparator = NaturalLanguageInvestmentStrategy.getLoanComparator();
                    return splitByRating.get(rating).stream().sorted(comparator);
                }).map(l -> { // recommend amount to invest per strategy
                    final int balance = portfolio.getCzkAvailable();
                    final int recommendedAmount = this.recommendInvestmentAmount(l.getLoan(), balance);
                    return l.recommend(recommendedAmount, this.needsConfirmation(l));
                }).flatMap(r -> r.map(Stream::of).orElse(Stream.empty())) // empty == not recommended
                .collect(Collectors.toList());
        NaturalLanguageInvestmentStrategy.LOGGER.debug("Strategy recommends the following marketplace: {}.",
                recommendations);
        return Collections.unmodifiableList(recommendations);
    }

    Optional<int[]> recommendInvestmentAmount(final Loan loan) {
        final Rating rating = loan.getRating();
        final int minimumInvestment = strategy.getMinimumInvestmentSizeInCzk(rating);
        final int maximumInvestment = strategy.getMaximumInvestmentSizeInCzk(rating);
        if (maximumInvestment < minimumInvestment) {
            return Optional.empty();
        }
        return Optional.of(new int[] {minimumInvestment, maximumInvestment});

    }

    int recommendInvestmentAmount(final Loan loan, final int balance) {
        return recommendInvestmentAmount(loan).map((int[] recommended) -> {
            final int minimumRecommendation = recommended[0];
            final int maximumRecommendation = recommended[1];
            NaturalLanguageInvestmentStrategy.LOGGER.trace("Recommended investment range for loan #{} is <{}; {}> CZK.",
                    loan.getId(), minimumRecommendation, maximumRecommendation);
            // round to nearest lower increment
            final int loanRemaining = (int)loan.getRemainingInvestment();
            if (minimumRecommendation > balance) {
                return 0;
            } else if (minimumRecommendation > loanRemaining) {
                return 0;
            }
            final int maxAllowedInvestmentIncrement = Defaults.MINIMUM_INVESTMENT_INCREMENT_IN_CZK;
            final int recommendedAmount = Math.min(balance, Math.min(maximumRecommendation, loanRemaining));
            final int result = (recommendedAmount / maxAllowedInvestmentIncrement) * maxAllowedInvestmentIncrement;
            if (result < minimumRecommendation) {
                return 0;
            } else {
                NaturalLanguageInvestmentStrategy.LOGGER.debug("Final recommendation for loan #{} is {} CZK.",
                        loan.getId(), result);
                return result;
            }
        }).orElse(0); // not recommended
    }

}
