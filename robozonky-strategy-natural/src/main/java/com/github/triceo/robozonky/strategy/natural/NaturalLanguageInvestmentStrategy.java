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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.RecommendedLoan;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NaturalLanguageInvestmentStrategy implements InvestmentStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(NaturalLanguageInvestmentStrategy.class);
    private static final Comparator<LoanDescriptor>
            BY_TERM = Comparator.comparingInt(l -> l.item().getTermInMonths()),
            BY_RECENCY = Comparator.comparing((LoanDescriptor l) -> l.item().getDatePublished()).reversed(),
            BY_REMAINING = Comparator.comparing((LoanDescriptor l) -> l.item().getRemainingInvestment()).reversed();

    private static int roundToNearestIncrement(final int number) {
        return roundToNearestIncrement(number, Defaults.MINIMUM_INVESTMENT_INCREMENT_IN_CZK);
    }

    private static int roundToNearestIncrement(final int number, final int increment) {
        return (number / increment) * increment;
    }

    private static Map<Rating, Collection<LoanDescriptor>> sortLoansByRating(final Stream<LoanDescriptor> loans) {
        return Collections.unmodifiableMap(loans.distinct().collect(Collectors.groupingBy(l -> l.item().getRating())));
    }

    /**
     * Pick a loan ordering such that it maximizes the chances the loan is still available on the marketplace when the
     * investment operation is triggered.
     * @return Comparator to order the marketplace with.
     */
    private static Comparator<LoanDescriptor> getLoanComparator() {
        return BY_TERM.thenComparing(BY_REMAINING).thenComparing(BY_RECENCY);
    }

    private final ParsedStrategy strategy;

    public NaturalLanguageInvestmentStrategy(final ParsedStrategy p) {
        this.strategy = p;
    }

    private boolean needsConfirmation(final LoanDescriptor loanDescriptor) {
        return strategy.needsConfirmation(loanDescriptor);
    }

    @Override
    public Stream<RecommendedLoan> recommend(final Collection<LoanDescriptor> loans,
                                             final PortfolioOverview portfolio) {
        if (!Util.isAcceptable(strategy, portfolio)) {
            LOGGER.debug("Not recommending anything due to unacceptable portfolio.");
            return Stream.empty();
        }
        // split available marketplace into buckets per rating
        final Map<Rating, Collection<LoanDescriptor>> splitByRating =
                NaturalLanguageInvestmentStrategy.sortLoansByRating(strategy.getApplicableLoans(loans));
        // prepare map of ratings and their shares
        final Map<Rating, BigDecimal> relevantPortfolio = splitByRating.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), portfolio::getShareOnInvestment));
        // and now return recommendations in the order in which investment should be attempted
        final int balance = portfolio.getCzkAvailable();
        return Util.rankRatingsByDemand(strategy, relevantPortfolio)
                .flatMap(rating -> { // prioritize marketplace by their ranking's demand
                    return splitByRating.get(rating).stream().sorted(getLoanComparator());
                })
                .peek(d -> LOGGER.trace("Evaluating {}.", d.item()))
                .map(l -> { // recommend amount to invest per strategy
                    final int recommendedAmount = recommendInvestmentAmount(l.item(), balance);
                    return l.recommend(recommendedAmount, needsConfirmation(l));
                }).flatMap(r -> r.map(Stream::of).orElse(Stream.empty()));
    }

    private int[] getRecommendationBoundaries(final Loan loan) {
        final Rating rating = loan.getRating();
        final int minimumInvestment = strategy.getMinimumInvestmentSizeInCzk(rating);
        final int maximumInvestment = strategy.getMaximumInvestmentSizeInCzk(rating);
        return new int[]{minimumInvestment, maximumInvestment};
    }

    private static int getPercentage(final double original, final int percentage) {
        return BigDecimal.valueOf(original)
                .multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN)
                .intValue();
    }

    int recommendInvestmentAmount(final Loan loan, final int balance) {
        final int id = loan.getId();
        return recommendInvestmentAmount(loan).map(recommended -> {
            final int minimumRecommendation = recommended[0];
            final int maximumRecommendation = recommended[1];
            LOGGER.trace("Recommended investment range for loan #{} is <{}; {}> CZK.", id, minimumRecommendation,
                         maximumRecommendation);
            // round to nearest lower increment
            final int loanRemaining = (int) loan.getRemainingInvestment();
            if (minimumRecommendation > balance) {
                LOGGER.trace("Not recommending loan #{} due to minimum over balance.", id);
                return 0;
            } else if (minimumRecommendation > loanRemaining) {
                LOGGER.trace("Not recommending loan #{} due to minimum over remaining.", id);
                return 0;
            }
            final int recommendedAmount = Math.min(balance, Math.min(maximumRecommendation, loanRemaining));
            final int r = roundToNearestIncrement(recommendedAmount);
            if (r < minimumRecommendation) {
                LOGGER.trace("Not recommending loan #{} due to recommendation below minimum.", id);
                return 0;
            } else {
                LOGGER.debug("Final recommendation for loan #{} is {} CZK.", id, r);
                return r;
            }
        }).orElse(0); // not recommended
    }

    private Optional<int[]> recommendInvestmentAmount(final Loan loan) {
        final int[] recommended = getRecommendationBoundaries(loan);
        final int minimumRecommendation =
                roundToNearestIncrement(Math.max(recommended[0], Defaults.MINIMUM_INVESTMENT_IN_CZK));
        final int maximumRecommendation = roundToNearestIncrement(recommended[1]);
        final int loanId = loan.getId();
        LOGGER.trace("Strategy gives investment range for loan #{} of <{}; {}> CZK.", loanId,
                     minimumRecommendation, maximumRecommendation);
        final int minimumInvestmentByShare =
                NaturalLanguageInvestmentStrategy.getPercentage(loan.getAmount(),
                                                                strategy.getMinimumInvestmentShareInPercent());
        final int minimumInvestment =
                Math.max(minimumInvestmentByShare, strategy.getMinimumInvestmentSizeInCzk(loan.getRating()));
        final int maximumInvestmentByShare =
                NaturalLanguageInvestmentStrategy.getPercentage(loan.getAmount(),
                                                                strategy.getMaximumInvestmentShareInPercent());
        final int maximumInvestment =
                Math.min(maximumInvestmentByShare, strategy.getMaximumInvestmentSizeInCzk(loan.getRating()));
        if (maximumInvestment < minimumInvestment) {
            return Optional.empty();
        }
        return Optional.of(new int[]{minimumInvestment, maximumInvestment});
    }
}
