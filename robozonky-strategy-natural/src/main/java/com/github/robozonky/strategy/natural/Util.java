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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

final class Util {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.TEN.pow(2);

    private Util() {
        // no instances
    }

    static <T> Map<Rating, List<T>> sortByRating(final Stream<T> items, final Function<T, Rating> ratingExtractor) {
        return items.collect(groupingBy(ratingExtractor,
                                        () -> new EnumMap<>(Rating.class),
                                        mapping(identity(), toList())));
    }

    static Stream<Rating> rankRatingsByDemand(final ParsedStrategy strategy, final Collection<Rating> ratings,
                                              final PortfolioOverview portfolio) {
        final SortedMap<BigDecimal, EnumSet<Rating>> mostWantedRatings = new TreeMap<>(Comparator.reverseOrder());
        // put the ratings into buckets based on how much we're missing them
        ratings.forEach(r -> {
            final BigDecimal currentRatingShare = portfolio.getShareOnInvestment(r);
            final BigDecimal maximumAllowedShare = divide(strategy.getMaximumShare(r), ONE_HUNDRED);
            final BigDecimal undershare = maximumAllowedShare.subtract(currentRatingShare);
            if (undershare.signum() < 1) { // we over-invested into this rating; do not include
                final BigDecimal pp = undershare.multiply(ONE_HUNDRED).negate();
                Decisions.report(logger -> logger.debug("Rating {} over-invested by {} percentage points.", r, pp));
                return;
            }
            // rank the rating
            mostWantedRatings.computeIfAbsent(undershare, k -> EnumSet.noneOf(Rating.class));
            mostWantedRatings.get(undershare).add(r);
            // inform that the rating is under-invested
            final BigDecimal minimumNeededShare = divide(strategy.getMinimumShare(r), ONE_HUNDRED);
            if (currentRatingShare.compareTo(minimumNeededShare) < 0) {
                final BigDecimal pp = minimumNeededShare.subtract(currentRatingShare).multiply(ONE_HUNDRED);
                Decisions.report(logger -> logger.debug("Rating {} under-invested by {} percentage points.", r, pp));
            }
        });
        return mostWantedRatings.values().stream().flatMap(Collection::stream);
    }

    static boolean isAcceptable(final ParsedStrategy strategy, final PortfolioOverview portfolio) {
        final long balance = portfolio.getCzkAvailable().longValue();
        if (balance < strategy.getMinimumBalance()) {
            Decisions.report(logger -> logger.debug("Not recommending any loans due to balance under minimum."));
            return false;
        }
        final long invested = portfolio.getCzkInvested().longValue();
        final long investmentCeiling = strategy.getMaximumInvestmentSizeInCzk();
        if (invested >= investmentCeiling) {
            Decisions.report(logger -> logger.debug("Not recommending any loans due to reaching the ceiling."));
            return false;
        }
        return true;
    }
}
