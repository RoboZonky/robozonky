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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.robozonky.strategy.natural.Audit.LOGGER;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

final class Util {

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
            final BigDecimal currentRatingShare = portfolio.getShareOnInvestment(r).asPercentage();
            final BigDecimal maximumAllowedShare = strategy.getMaximumShare(r).asPercentage();
            final BigDecimal undershare = maximumAllowedShare.subtract(currentRatingShare);
            if (undershare.signum() < 1) { // we over-invested into this rating; do not include
                final BigDecimal pp = undershare.negate();
                LOGGER.debug("Rating {} over-invested by {} percentage point(s).", r, pp);
                return;
            }
            // rank the rating
            mostWantedRatings.computeIfAbsent(undershare, k -> EnumSet.noneOf(Rating.class));
            mostWantedRatings.get(undershare).add(r);
            // inform that the rating is under-invested
            final BigDecimal minimumNeededShare = strategy.getMinimumShare(r).asPercentage();
            if (currentRatingShare.compareTo(minimumNeededShare) < 0) {
                final BigDecimal pp = minimumNeededShare.subtract(currentRatingShare);
                LOGGER.debug("Rating {} under-invested by {} percentage point(s).", r, pp);
            }
        });
        return mostWantedRatings.values().stream().flatMap(Collection::stream);
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
