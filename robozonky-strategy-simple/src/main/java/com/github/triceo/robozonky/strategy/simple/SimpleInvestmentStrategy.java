/*
 * Copyright 2016 Lukáš Petrovický
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimpleInvestmentStrategy implements InvestmentStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleInvestmentStrategy.class);
    private static final Comparator<LoanDescriptor> BY_TERM =
            Comparator.comparingInt(l -> l.getLoan().getTermInMonths());

    static Map<Rating, Collection<LoanDescriptor>> sortLoansByRating(final Collection<LoanDescriptor> loans) {
        return Collections.unmodifiableMap(loans.stream().distinct()
                .collect(Collectors.groupingBy(l -> l.getLoan().getRating())));
    }

    private List<Rating> rankRatingsByDemand(final Map<Rating, BigDecimal> currentShare,
                                             final Function<StrategyPerRating, BigDecimal> metric) {
        final SortedMap<BigDecimal, List<Rating>> mostWantedRatings = new TreeMap<>(Comparator.reverseOrder());
        // put the ratings into buckets based on how much we're missing them
        currentShare.forEach((r, currentRatingShare) -> {
            final BigDecimal maximumAllowedShare = metric.apply(this.individualStrategies.get(r));
            final BigDecimal undershare = maximumAllowedShare.subtract(currentRatingShare);
            if (undershare.compareTo(BigDecimal.ZERO) <= 0) { // we over-invested into this rating; do not include
                return;
            }
            mostWantedRatings.compute(undershare, (k, v) -> { // each rating unique at source, so list works
                final List<Rating> target = (v == null) ? new ArrayList<>(1) : v;
                target.add(r);
                return target;
            });
        });
        return mostWantedRatings.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    /**
     * Ranks the ratings in the portfolio which need to have their overall share increased. First on this list will be
     * ratings which are under their target share. Following them will be ratings which are under their maximum share.
     * Ratings with their respective shares over the maximum will not be present.
     *
     * @param currentShare Current share of investments in a given rating.
     * @return Ratings in the order of decreasing demand.
     */
    List<Rating> rankRatingsByDemand(final Map<Rating, BigDecimal> currentShare) {
        // find out which ratings are under-invested
        final List<Rating> ratingsUnderTarget = rankRatingsByDemand(currentShare, StrategyPerRating::getTargetShare);
        SimpleInvestmentStrategy.LOGGER.info("Ratings under-invested: {}.", ratingsUnderTarget);
        // find out which other ratings are not yet maxed out
        final Map<Rating, BigDecimal> filteredShare = currentShare.entrySet()
                .stream()
                .filter(e -> !ratingsUnderTarget.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        final List<Rating> ratingsUnderMaximum = rankRatingsByDemand(filteredShare, StrategyPerRating::getMaximumShare);
        Collections.reverse(ratingsUnderMaximum); // the closer we get to maximum share, the less desirable
        SimpleInvestmentStrategy.LOGGER.info("Ratings not yet over-invested: {}.", ratingsUnderMaximum);
        // merge both and produce result
        final List<Rating> result = Stream.concat(ratingsUnderTarget.stream(), ratingsUnderMaximum.stream())
                .collect(Collectors.toList());
        return Collections.unmodifiableList(result);
    }

    private final int minimumBalance, investmentCeiling;
    private final Map<Rating, StrategyPerRating> individualStrategies = new EnumMap<>(Rating.class);

    SimpleInvestmentStrategy(final int minimumBalance, final int investmentCeiling,
                             final Map<Rating, StrategyPerRating> individualStrategies) {
        this.minimumBalance = minimumBalance;
        this.investmentCeiling = investmentCeiling;
        for (final Rating r: Rating.values()) {
            if (!individualStrategies.containsKey(r)) {
                throw new IllegalArgumentException("Missing strategy for rating " + r);
            }
            final StrategyPerRating s = individualStrategies.get(r);
            this.individualStrategies.put(r, s);
        }
    }

    private boolean isAcceptable(final PortfolioOverview portfolio) {
        final int availableBalance = portfolio.getCzkAvailable();
        if (availableBalance < this.minimumBalance) {
            SimpleInvestmentStrategy.LOGGER.debug("According to the investment strategy, {} CZK balance is less than "
                    + "minimum {} CZK. Not recommending any loans.", availableBalance, this.minimumBalance);
            return false;
        }
        final int invested = portfolio.getCzkInvested();
        if (invested > this.investmentCeiling) {
            SimpleInvestmentStrategy.LOGGER.debug("According to the investment strategy, {} CZK total investment "
                    + "exceeds {} CZK ceiling. Not recommending any loans.", invested, this.investmentCeiling);
            return false;
        }
        return true;
    }

    private boolean needsConfirm(final LoanDescriptor loanDescriptor) {
        final Rating r = loanDescriptor.getLoan().getRating();
        return this.individualStrategies.get(r).isConfirmationRequired();
    }

    @Override
    public List<Recommendation> recommend(final Collection<LoanDescriptor> availableLoans,
                                          final PortfolioOverview portfolio) {
        if (!this.isAcceptable(portfolio)) {
            return Collections.emptyList();
        }
        final Map<Rating, Collection<LoanDescriptor>> splitByRating =
                SimpleInvestmentStrategy.sortLoansByRating(availableLoans);
        final List<LoanDescriptor> acceptableLoans =
                this.rankRatingsByDemand(portfolio.getSharesOnInvestment()).stream()
                        .filter(splitByRating::containsKey)
                        .map(this.individualStrategies::get)
                        .flatMap(strategy -> {
                            final Comparator<LoanDescriptor> properOrder = strategy.isLongerTermPreferred() ?
                                    SimpleInvestmentStrategy.BY_TERM.reversed() : SimpleInvestmentStrategy.BY_TERM;
                            return splitByRating.get(strategy.getRating()).stream()
                                    .filter(l -> strategy.isAcceptable(l.getLoan()))
                                    .sorted(properOrder);
                        }).collect(Collectors.toList());
        SimpleInvestmentStrategy.LOGGER.debug("Strategy recommends the following unseen loans: {}.", acceptableLoans);
        return Collections.unmodifiableList(acceptableLoans.stream()
                .map(l -> l.recommend(this.recommendInvestmentAmount(l.getLoan(), portfolio), this.needsConfirm(l)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList()));
    }

    int recommendInvestmentAmount(final Loan loan, final PortfolioOverview portfolio) {
        if (!this.isAcceptable(portfolio)) {
            return 0;
        }
        final Optional<int[]> recommend = individualStrategies.get(loan.getRating()).recommendInvestmentAmount(loan);
        if (!recommend.isPresent()) { // does not match the strategy
            return 0;
        }
        final int minimumRecommendation = recommend.get()[0];
        final int maximumRecommendation = recommend.get()[1];
        SimpleInvestmentStrategy.LOGGER.trace("Recommended investment for loan #{} in range of <{}; {}> CZK.",
                loan.getId(), minimumRecommendation, maximumRecommendation);
        // round to nearest lower increment
        final int balance = portfolio.getCzkAvailable();
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
            SimpleInvestmentStrategy.LOGGER.debug("Final recommendation for loan #{} is {} CZK.", loan.getId(), result);
            return result;
        }
    }

}
