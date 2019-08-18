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

import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;

import static com.github.robozonky.strategy.natural.Audit.LOGGER;

class NaturalLanguageInvestmentStrategy implements InvestmentStrategy {

    private static final Comparator<LoanDescriptor> COMPARATOR = new PrimaryMarketplaceComparator();

    private final ParsedStrategy strategy;
    private final InvestmentSizeRecommender recommender;

    public NaturalLanguageInvestmentStrategy(final ParsedStrategy p) {
        this.strategy = p;
        this.recommender = new InvestmentSizeRecommender(p);
    }

    @Override
    public Stream<RecommendedLoan> recommend(final Collection<LoanDescriptor> available,
                                             final PortfolioOverview portfolio, final Restrictions restrictions) {
        if (!Util.isAcceptable(strategy, portfolio)) {
            return Stream.empty();
        }
        // split available marketplace into buckets per rating
        final Map<Rating, List<LoanDescriptor>> splitByRating =
                Util.sortByRating(strategy.getApplicableLoans(available, portfolio), d -> d.item().getRating());
        // and now return recommendations in the order in which investment should be attempted
        return Util.rankRatingsByDemand(strategy, splitByRating.keySet(), portfolio)
                .peek(rating -> LOGGER.trace("Processing rating {}.", rating))
                .flatMap(rating -> splitByRating.get(rating).stream().sorted(COMPARATOR))
                .peek(d -> LOGGER.trace("Evaluating {}.", d.item()))
                .flatMap(d -> { // recommend amount to invest per strategy
                    final int recommendedAmount = recommender.apply(d.item(), restrictions);
                    if (recommendedAmount > 0) {
                        return d.recommend(recommendedAmount).stream();
                    } else {
                        return Stream.empty();
                    }
                });
    }
}
