/*
 * Copyright 2021 The RoboZonky Project
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

import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.strategies.PortfolioOverview;

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

    private static final Logger LOGGER = Audit.selling();
    private static final Ratio MAX_SELLOFF_SHARE_PER_RATING = Ratio.fromPercentage(0.5);

    private static Stream<RecommendedInvestment> determineSelloffByRating(final List<RecommendedInvestment> eligible,
            final Money maxSelloffSize) {
        var czkIncluded = maxSelloffSize.getZero();
        eligible.sort(Comparator.comparing(d -> d.descriptor()
            .item()
            .getPrincipal()
            .getUnpaid()));
        LOGGER.trace("Eligible investments: {}.", eligible);
        // Find all the investments that can be sold without reaching over the limit, start with the smallest first.
        var firstUnacceptableInvestmentIndex = -1;
        for (var i = 0; i < eligible.size(); i++) {
            var investment = eligible.get(i);
            var value = investment.descriptor()
                .item()
                .getPrincipal()
                .getUnpaid();
            var ifIncluded = czkIncluded.add(value);
            if (ifIncluded.compareTo(maxSelloffSize) > 0) {
                firstUnacceptableInvestmentIndex = i;
                break;
            }
            czkIncluded = czkIncluded.add(value);
        }
        if (firstUnacceptableInvestmentIndex == 0) {
            /*
             * If no investments can be sold without reaching over the limit, then the portfolio is very small.
             * Still, the user expects us to sell something, so pick the smallest thing we could possibly sell.
             */
            var descriptor = eligible.get(0);
            LOGGER.debug("Will sell one investment: {}.", descriptor);
            return Stream.of(descriptor);
        } else {
            var toBeSold = eligible.subList(0, firstUnacceptableInvestmentIndex);
            LOGGER.debug("Investments with total value of {} to be sold: {}.", czkIncluded, toBeSold);
            return toBeSold.stream();
        }
    }

    @Override
    public Stream<RecommendedInvestment> apply(final Stream<RecommendedInvestment> investmentDescriptors,
            final PortfolioOverview portfolioOverview) {
        LOGGER.debug("Starting to query for sellable investments.");
        var eligible = investmentDescriptors
            .collect(Collectors.groupingBy(t -> t.descriptor()
                .item()
                .getLoan()
                .getInterestRate(), Collectors.toList()));
        if (eligible.isEmpty()) {
            LOGGER.debug("No investments eligible for sale.");
            return Stream.empty();
        }
        LOGGER.debug("Throttling the selling algorithm.");
        var maxSeloffValue = MAX_SELLOFF_SHARE_PER_RATING.apply(portfolioOverview.getInvested());
        return eligible.entrySet()
            .stream()
            .flatMap(e -> {
                LOGGER.debug("Processing investments with interest rate {} of up to {}.", e.getKey(), maxSeloffValue);
                return determineSelloffByRating(e.getValue(), maxSeloffValue);
            });
    }
}
