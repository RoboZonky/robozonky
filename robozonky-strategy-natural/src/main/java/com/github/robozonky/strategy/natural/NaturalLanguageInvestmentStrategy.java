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

import static com.github.robozonky.strategy.natural.Audit.LOGGER;

import java.util.stream.Stream;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.util.functional.Tuple;
import com.github.robozonky.internal.util.functional.Tuple2;

class NaturalLanguageInvestmentStrategy implements InvestmentStrategy {

    private final ParsedStrategy strategy;
    private final InvestmentSizeRecommender recommender;

    public NaturalLanguageInvestmentStrategy(final ParsedStrategy p) {
        this.strategy = p;
        this.recommender = new InvestmentSizeRecommender(p);
    }

    @Override
    public Stream<Tuple2<LoanDescriptor, Money>> recommend(final Stream<LoanDescriptor> available,
            final PortfolioOverview portfolio,
            final SessionInfo sessionInfo) {
        if (!Util.isAcceptable(strategy, portfolio)) {
            return Stream.empty();
        }
        var preferences = Preferences.get(strategy, portfolio);
        var withoutUndesirable = available.parallel()
            .peek(d -> LOGGER.trace("Evaluating {}.", d.item()))
            .filter(d -> { // skip loans in ratings which are not required by the strategy
                boolean isAcceptable = preferences.getDesirableRatings()
                    .contains(d.item()
                        .getRating());
                if (!isAcceptable) {
                    LOGGER.debug("Loan #{} skipped due to an undesirable rating.", d.item()
                        .getId());
                }
                return isAcceptable;
            });
        return strategy.getApplicableLoans(withoutUndesirable, portfolio)
            .sorted(preferences.getPrimaryMarketplaceComparator())
            .flatMap(d -> { // recommend amount to invest per strategy
                final Money recommendedAmount = recommender.apply(d.item(), sessionInfo);
                if (!recommendedAmount.isZero()) {
                    return Stream.of(Tuple.of(d, recommendedAmount));
                } else {
                    return Stream.empty();
                }
            });
    }
}
