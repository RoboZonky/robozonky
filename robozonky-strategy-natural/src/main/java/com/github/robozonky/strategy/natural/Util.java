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
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import org.eclipse.collections.impl.map.sorted.mutable.TreeSortedMap;

public class Util {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.TEN.pow(2);

    private static BigDecimal toDecimalShare(final int integerShare) {
        return BigDecimal.valueOf(integerShare).divide(ONE_HUNDRED, 4, RoundingMode.HALF_EVEN);
    }

    public static Stream<Rating> rankRatingsByDemand(final ParsedStrategy strategy,
                                                     final Map<Rating, BigDecimal> currentShare) {
        final SortedMap<BigDecimal, EnumSet<Rating>> mostWantedRatings = new TreeSortedMap<>(Comparator.reverseOrder());
        // put the ratings into buckets based on how much we're missing them
        currentShare.forEach((r, currentRatingShare) -> {
            final BigDecimal maximumAllowedShare = toDecimalShare(strategy.getMaximumShare(r));
            final BigDecimal undershare = maximumAllowedShare.subtract(currentRatingShare);
            if (undershare.signum() == -1) { // we over-invested into this rating; do not include
                final BigDecimal pp = undershare.multiply(ONE_HUNDRED).negate();
                Decisions.report(logger -> logger.debug("Rating {} over-invested by {} percentage points.", r, pp));
                return;
            }
            mostWantedRatings.compute(undershare, (k, v) -> { // rank the rating
                if (v == null) {
                    return EnumSet.of(r);
                }
                v.add(r);
                return v;
            });
            final BigDecimal minimumNeededShare = toDecimalShare(strategy.getMinimumShare(r));
            if (currentRatingShare.compareTo(minimumNeededShare) < 0) { // inform that the rating is under-invested
                final BigDecimal pp = minimumNeededShare.subtract(currentRatingShare).multiply(ONE_HUNDRED);
                Decisions.report(logger -> logger.debug("Rating {} under-invested by {} percentage points.", r, pp));
            }
        });
        return mostWantedRatings.values().stream().flatMap(Collection::stream);
    }

    public static boolean isAcceptable(final ParsedStrategy strategy, final PortfolioOverview portfolio) {
        final int balance = portfolio.getCzkAvailable();
        if (balance < strategy.getMinimumBalance()) {
            Decisions.report(logger -> logger.debug("Not recommending any loans due to balance under minimum."));
            return false;
        }
        final int invested = portfolio.getCzkInvested();
        final int investmentCeiling = strategy.getMaximumInvestmentSizeInCzk();
        if (invested >= investmentCeiling) {
            Decisions.report(logger -> logger.debug("Not recommending any loans due to reaching the ceiling."));
            return false;
        }
        return true;
    }

    public static BigDecimal toBigDecimal(final Number num) {
        return new BigDecimal(num.toString());
    }
}
