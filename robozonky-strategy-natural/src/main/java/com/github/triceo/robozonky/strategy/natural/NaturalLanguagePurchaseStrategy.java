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

import com.github.triceo.robozonky.api.remote.entities.Participation;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.ParticipationDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.PurchaseStrategy;
import com.github.triceo.robozonky.api.strategies.RecommendedParticipation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NaturalLanguagePurchaseStrategy implements PurchaseStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(NaturalLanguagePurchaseStrategy.class);
    private static final Comparator<ParticipationDescriptor>
            BY_TERM = Comparator.comparingInt(p -> p.item().getRemainingInstalmentCount()),
            BY_RECENCY = Comparator.comparing(p -> p.item().getDeadline()),
            BY_REMAINING = Comparator.comparing(
                    (ParticipationDescriptor p) -> p.item().getRemainingPrincipal()).reversed();

    private static Map<Rating, Collection<ParticipationDescriptor>> sortByRating(
            final Stream<ParticipationDescriptor> items) {
        return Collections.unmodifiableMap(items.distinct().collect(Collectors.groupingBy(l -> l.item().getRating())));
    }

    /**
     * Pick a loan ordering such that it maximizes the chances the loan is still available on the marketplace when the
     * investment operation is triggered.
     * @return Comparator to order the marketplace with.
     */
    private static Comparator<ParticipationDescriptor> getLoanComparator() {
        return BY_TERM.thenComparing(BY_REMAINING).thenComparing(BY_RECENCY);
    }

    private final ParsedStrategy strategy;

    public NaturalLanguagePurchaseStrategy(final ParsedStrategy p) {
        this.strategy = p;
    }

    private boolean isAcceptable(final PortfolioOverview portfolio) {
        final int balance = portfolio.getCzkAvailable();
        if (balance < strategy.getMinimumBalance()) {
            LOGGER.debug("{} CZK balance is less than minimum {} CZK.", balance, strategy.getMinimumBalance());
            return false;
        }
        final int invested = portfolio.getCzkInvested();
        final int investmentCeiling = strategy.getMaximumInvestmentSizeInCzk();
        if (invested >= investmentCeiling) {
            LOGGER.debug("{} CZK total investment over {} CZK ceiling.", invested, investmentCeiling);
            return false;
        }
        return true;
    }

    private int[] getRecommendationBoundaries(final Participation participation) {
        final Rating rating = participation.getRating();
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

    boolean sizeMatchesStrategy(final Participation participation, final int balance) {
        return recommendInvestmentAmount(participation).map(recommended -> {
            final int minimumRecommendation = recommended[0];
            final int maximumRecommendation = recommended[1];
            LOGGER.trace("Recommended investment range for loan #{} is <{}; {}> CZK.", participation.getId(),
                         minimumRecommendation, maximumRecommendation);
            // round to nearest lower increment
            final double price = participation.getRemainingPrincipal().doubleValue();
            if (balance < price) {
                LOGGER.debug("Loan #{} not recommended due to low balance.", participation.getLoanId());
                return false;
            } else if (minimumRecommendation > price) {
                LOGGER.debug("Loan #{} not recommended due to price below minimum.", participation.getLoanId());
                return false;
            } else if (price > maximumRecommendation) {
                LOGGER.debug("Loan #{} not recommended due to price over maximum.", participation.getLoanId());
                return false;
            } else {
                LOGGER.debug("Final recommendation for loan #{} is to buy.", participation.getLoanId());
                return true;
            }
        }).orElse(false); // not recommended
    }

    private Optional<int[]> recommendInvestmentAmount(final Participation loan) {
        final int[] recommended = getRecommendationBoundaries(loan);
        final int minimumRecommendation = recommended[0];
        final int maximumRecommendation = recommended[1];
        final int loanId = loan.getId();
        LOGGER.trace("Strategy gives investment range for loan #{} of <{}; {}> CZK.", loanId, minimumRecommendation,
                     maximumRecommendation);
        final double remainingPrincipal = loan.getRemainingPrincipal().doubleValue();
        final int minimumInvestmentByShare =
                getPercentage(remainingPrincipal, strategy.getMinimumInvestmentShareInPercent());
        final int minimumInvestment =
                Math.max(minimumInvestmentByShare, strategy.getMinimumInvestmentSizeInCzk(loan.getRating()));
        final int maximumInvestmentByShare =
                getPercentage(remainingPrincipal, strategy.getMaximumInvestmentShareInPercent());
        final int maximumInvestment =
                Math.min(maximumInvestmentByShare, strategy.getMaximumInvestmentSizeInCzk(loan.getRating()));
        if (maximumInvestment < minimumInvestment) {
            return Optional.empty();
        }
        return Optional.of(new int[]{minimumInvestment, maximumInvestment});
    }

    @Override
    public Stream<RecommendedParticipation> recommend(final Collection<ParticipationDescriptor> available,
                                                      final PortfolioOverview portfolio) {
        if (!this.isAcceptable(portfolio)) {
            return Stream.empty();
        }
        // split available marketplace into buckets per rating
        final Map<Rating, Collection<ParticipationDescriptor>> splitByRating =
                sortByRating(strategy.getApplicableParticipations(available));
        // prepare map of ratings and their shares
        final Map<Rating, BigDecimal> relevantPortfolio = splitByRating.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), portfolio::getShareOnInvestment));
        // recommend amount to invest per strategy
        return Util.rankRatingsByDemand(strategy, relevantPortfolio)
                .flatMap(rating -> { // prioritize marketplace by their ranking's demand
                    return splitByRating.get(rating).stream().sorted(getLoanComparator());
                })
                .filter(d -> sizeMatchesStrategy(d.item(), portfolio.getCzkAvailable()))
                .map(ParticipationDescriptor::recommend) // must do full amount; Zonky enforces
                .flatMap(r -> r.map(Stream::of).orElse(Stream.empty()));
    }
}
