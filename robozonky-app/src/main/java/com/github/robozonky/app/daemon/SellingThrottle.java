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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.internal.util.BigDecimalCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The goal of this class is to only sell investments in small amounts, so that the entire {@link PortfolioOverview} is
 * not destabilized.
 * <p>
 * This is necessary, since Zonky only recalculates {@link PortfolioOverview} every 2 hours and investments sold before
 * the recalculation therefore cause {@link PortfolioOverview} to become out of sync with reality. Since the selling
 * operation is fundamentally asynchronous (we offer to sell, then someone in the future may decide to buy), we can not
 * "fix" portfolio on our side, as that would require us to constantly query for sold investments, making sure we have
 * included all of them in {@link PortfolioOverview}.
 * <p>
 * This throttle will make sure we only sell so much at once as not to affect the portfolio too much.
 * {@link SellingJob} will ensure that the operation happens often enough that even large selloffs can be accomplished
 * within days at worst.
 */
final class SellingThrottle
        implements BiFunction<Stream<RecommendedInvestment>, PortfolioOverview, Stream<RecommendedInvestment>> {

    private static final Logger LOGGER = LogManager.getLogger(SellingThrottle.class);
    private static final Ratio MAX_SELLOFF_SHARE_PER_RATING = Ratio.fromPercentage(0.5);

    private static Stream<RecommendedInvestment> determineSelloffByRating(final Set<RecommendedInvestment> eligible,
                                                                          final long maxSelloffSizeInCzk) {
        if (eligible.isEmpty()) {
            LOGGER.debug("No investments eligible.");
            return Stream.empty();
        }
        long czkIncluded = 0;
        final List<RecommendedInvestment> byAmountIncreasing = eligible.stream()
                .sorted(Comparator.comparing(d -> d.descriptor().item().getRemainingPrincipal()))
                .collect(Collectors.toList());
        LOGGER.trace("Eligible investments: {}.", byAmountIncreasing);
        final Set<RecommendedInvestment> included = new HashSet<>();
        // find all the investments that can be sold without reaching over the limit, start with the smallest first
        for (final RecommendedInvestment evaluating : byAmountIncreasing) {
            final long value = evaluating.descriptor().item().getRemainingPrincipal().longValue();
            final long ifIncluded = czkIncluded + value;
            if (ifIncluded > maxSelloffSizeInCzk) {
                continue;
            }
            czkIncluded += value;
            included.add(evaluating);
        }
        /*
         * if no investments can be sold without reaching over the limit, then the portfolio is very small; still, the
         * user expects us to sell something, so pick the smallest thing we could possibly sell.
         */
        if (included.isEmpty()) {
            final RecommendedInvestment descriptor = byAmountIncreasing.get(0);
            LOGGER.debug("Will sell one investment: {}.", descriptor);
            return Stream.of(descriptor);
        } else {
            LOGGER.debug("Investments with total value of {} CZK to be sold: {}.", czkIncluded, byAmountIncreasing);
            return included.stream();
        }
    }

    private static long getMaxSelloffValue(final PortfolioOverview portfolioOverview) {
        final BigDecimal invested = portfolioOverview.getCzkInvested();
        final BigDecimal sellable = BigDecimalCalculator.times(invested, MAX_SELLOFF_SHARE_PER_RATING);
        return sellable.longValue();
    }

    @Override
    public Stream<RecommendedInvestment> apply(final Stream<RecommendedInvestment> investmentDescriptors,
                                               final PortfolioOverview portfolioOverview) {
        final Map<Rating, Set<RecommendedInvestment>> eligible = investmentDescriptors
                .collect(Collectors.groupingBy(t -> t.descriptor().item().getRating(), Collectors.toSet()));
        final long maxSeloffValue = getMaxSelloffValue(portfolioOverview);
        return eligible.entrySet().stream()
                .flatMap(e -> {
                    final Rating r = e.getKey();
                    LOGGER.debug("Processing {} investments.", r);
                    return determineSelloffByRating(e.getValue(), maxSeloffValue);
                });
    }
}
