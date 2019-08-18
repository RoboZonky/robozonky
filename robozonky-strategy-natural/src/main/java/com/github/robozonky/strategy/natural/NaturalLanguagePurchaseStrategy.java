/*
 * Copyright 2019 The RoboZonky Project
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.RecommendedParticipation;

import static com.github.robozonky.strategy.natural.Audit.LOGGER;

class NaturalLanguagePurchaseStrategy implements PurchaseStrategy {

    private static final Comparator<ParticipationDescriptor> COMPARATOR = new SecondaryMarketplaceComparator();

    private final ParsedStrategy strategy;

    public NaturalLanguagePurchaseStrategy(final ParsedStrategy p) {
        this.strategy = p;
    }

    private int[] getRecommendationBoundaries(final Participation participation) {
        final Rating rating = participation.getRating();
        final int minimumInvestment = strategy.getMinimumInvestmentSizeInCzk(rating);
        final int maximumInvestment = strategy.getMaximumInvestmentSizeInCzk(rating);
        return new int[]{minimumInvestment, maximumInvestment};
    }

    boolean sizeMatchesStrategy(final Participation participation) {
        final int id = participation.getLoanId();
        final long participationId = participation.getId();
        final int[] recommended = getRecommendationBoundaries(participation);
        final int minimumRecommendation = recommended[0];
        final int maximumRecommendation = recommended[1];
        LOGGER.trace("Loan #{} (participation #{}) recommended range <{}; {}> CZK.", id, participationId,
                     minimumRecommendation, maximumRecommendation);
        // round to nearest lower increment
        final double price = participation.getRemainingPrincipal().doubleValue();
        if (minimumRecommendation > price) {
            LOGGER.debug("Loan #{} (participation #{}) not recommended; below minimum.", id, participationId);
        } else if (price > maximumRecommendation) {
            LOGGER.debug("Loan #{} (participation #{}) not recommended; over maximum.", id, participationId);
        } else {
            LOGGER.debug("Final recommendation: buy loan #{} (participation #{}).", id, participationId);
            return true;
        }
        return false;
    }

    @Override
    public Stream<RecommendedParticipation> recommend(final Collection<ParticipationDescriptor> available,
                                                      final PortfolioOverview portfolio,
                                                      final Restrictions restrictions) {
        if (!Util.isAcceptable(strategy, portfolio)) {
            return Stream.empty();
        }
        // split available marketplace into buckets per rating
        final Map<Rating, List<ParticipationDescriptor>> splitByRating =
                Util.sortByRating(strategy.getApplicableParticipations(available, portfolio),
                                  d -> d.item().getRating());
        // recommend amount to invest per strategy
        return Util.rankRatingsByDemand(strategy, splitByRating.keySet(), portfolio)
                .flatMap(rating -> splitByRating.get(rating).stream().sorted(COMPARATOR))
                .peek(d -> LOGGER.trace("Evaluating {}.", d.item()))
                .filter(d -> sizeMatchesStrategy(d.item()))
                .flatMap(d -> d.recommend().map(Stream::of).orElse(Stream.empty()));
    }
}
