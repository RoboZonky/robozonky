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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NaturalLanguageInvestmentStrategy implements InvestmentStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(NaturalLanguageInvestmentStrategy.class);
    private static final PrimaryMarketplaceComparator COMPARATOR = new PrimaryMarketplaceComparator();
    private final ParsedStrategy strategy;
    private final InvestmentSizeRecommender recommender;

    public NaturalLanguageInvestmentStrategy(final ParsedStrategy p) {
        this.strategy = p;
        this.recommender = new InvestmentSizeRecommender(p);
    }

    private static Map<Rating, Collection<LoanDescriptor>> sortLoansByRating(final Stream<LoanDescriptor> loans) {
        return Collections.unmodifiableMap(loans.distinct().collect(Collectors.groupingBy(l -> l.item().getRating())));
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
                .flatMap(rating -> splitByRating.get(rating).stream().sorted(COMPARATOR))
                .peek(d -> LOGGER.trace("Evaluating {}.", d.item()))
                .map(l -> { // recommend amount to invest per strategy
                    final int recommendedAmount = recommender.apply(l.item(), balance);
                    if (recommendedAmount > 0) {
                        return l.recommend(recommendedAmount, needsConfirmation(l));
                    } else {
                        return Optional.<RecommendedLoan>empty();
                    }
                }).flatMap(r -> r.map(Stream::of).orElse(Stream.empty()));
    }
}
