/*
 * Copyright 2020 The RoboZonky Project
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
import java.util.stream.Stream;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.RecommendedParticipation;

import static com.github.robozonky.strategy.natural.Audit.LOGGER;

class NaturalLanguagePurchaseStrategy implements PurchaseStrategy {

    private final ParsedStrategy strategy;

    public NaturalLanguagePurchaseStrategy(final ParsedStrategy p) {
        this.strategy = p;
    }

    private Money[] getRecommendationBoundaries(final Participation participation) {
        final Rating rating = participation.getRating();
        final Money minimumInvestment = strategy.getMinimumPurchaseSize(rating);
        final Money maximumInvestment = strategy.getMaximumPurchaseSize(rating);
        return new Money[]{minimumInvestment, maximumInvestment};
    }

    boolean sizeMatchesStrategy(final Participation participation) {
        final int id = participation.getLoanId();
        final long participationId = participation.getId();
        final Money[] recommended = getRecommendationBoundaries(participation);
        final Money minimumRecommendation = recommended[0];
        final Money maximumRecommendation = recommended[1];
        LOGGER.trace("Loan #{} (participation #{}) recommended range <{}; {}>.", id, participationId,
                     minimumRecommendation, maximumRecommendation);
        // round to nearest lower increment
        final Money price = participation.getRemainingPrincipal();
        if (minimumRecommendation.compareTo(price) > 0) {
            LOGGER.debug("Loan #{} (participation #{}) not recommended; below minimum.", id, participationId);
        } else if (price.compareTo(maximumRecommendation) > 0) {
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
        var preferences = Preferences.get(strategy, portfolio);
        var withoutUndesirable = available.parallelStream()
                .peek(d -> LOGGER.trace("Evaluating {}.", d.item()))
                .filter(d -> { // skip loans in ratings which are not required by the strategy
                    boolean isAcceptable = preferences.getDesirableRatings().contains(d.item().getRating());
                    if (!isAcceptable) {
                        LOGGER.debug("Participation #{} skipped due to an undesirable rating.", d.item().getId());
                    }
                    return isAcceptable;
                });
        return strategy.getApplicableParticipations(withoutUndesirable, portfolio)
                .filter(d -> sizeMatchesStrategy(d.item()))
                .sorted(preferences.getSecondaryMarketplaceComparator())
                .flatMap(d -> d.recommend().stream());
    }
}
