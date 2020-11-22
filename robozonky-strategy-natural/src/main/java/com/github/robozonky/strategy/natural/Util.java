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

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;
import static com.github.robozonky.strategy.natural.Audit.LOGGER;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class Util {

    private static final BigDecimal HUNDRED = BigDecimal.TEN.pow(2);

    private Util() {
        // no instances
    }

    static Set<Rating> getRatingsInDemand(final ParsedStrategy strategy, final PortfolioOverview portfolio) {
        var ratings = EnumSet.allOf(Rating.class);
        // put the ratings into buckets based on how much we're missing them
        for (var rating : Rating.values()) {
            var permittedShare = strategy.getPermittedShare(rating);
            if (permittedShare.compareTo(Ratio.ZERO) <= 0) { // prevent division by zero later
                LOGGER.debug("Rating {} permitted share is {} %, skipping.", rating, permittedShare.asPercentage());
                ratings.remove(rating);
                continue;
            }
            // under 0 = underinvested, over 0 = overinvested
            var currentRatingShare = portfolio.getShareOnInvestment(rating);
            var ratio = Ratio.fromRaw(divide(currentRatingShare.bigDecimalValue(),
                    permittedShare.bigDecimalValue()));
            var overinvested = ratio.compareTo(Ratio.ONE) >= 0;
            if (overinvested) { // we over-invested into this rating; do not include
                var percentOver = ratio.asPercentage()
                    .subtract(HUNDRED);
                LOGGER.debug("Rating {} over-invested by {} %, skipping. (Expected {}, got {}.)", rating, percentOver,
                        permittedShare, currentRatingShare);
                ratings.remove(rating);
                continue;
            }
            LOGGER.debug("Rating {} under-invested by {} %. (Expected {}, got {}.)", rating,
                    HUNDRED.subtract(ratio.asPercentage()), permittedShare, currentRatingShare);
        }
        return ratings;
    }

    static boolean isAcceptable(final ParsedStrategy strategy, final PortfolioOverview portfolio) {
        final Money invested = portfolio.getInvested();
        final Money investmentCeiling = strategy.getMaximumInvestmentSize();
        if (invested.compareTo(investmentCeiling) >= 0) {
            LOGGER.debug("Not recommending any loans due to reaching the ceiling.");
            return false;
        }
        return true;
    }
}
