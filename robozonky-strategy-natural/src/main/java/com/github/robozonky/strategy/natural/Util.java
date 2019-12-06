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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;
import static com.github.robozonky.strategy.natural.Audit.LOGGER;

final class Util {

    private static final BigDecimal HUNDRED = BigDecimal.TEN.pow(2);

    private Util() {
        // no instances
    }

    static Comparator<Rating> getRatingByDemandComparator(final Set<Rating> ratingsInOrderOfPreference) {
        Map<Rating, Integer> ratingRanking = new EnumMap<>(Rating.class);
        AtomicInteger rank = new AtomicInteger();
        ratingsInOrderOfPreference.forEach(r -> ratingRanking.computeIfAbsent(r, key -> rank.getAndIncrement()));
        return Comparator.comparingInt(o -> ratingRanking.getOrDefault(o, Integer.MAX_VALUE));
    }

    /**
     *
     * @param strategy
     * @param portfolio
     * @return More demanded ratings come first.
     */
    static Set<Rating> rankRatingsByDemand(final ParsedStrategy strategy, final PortfolioOverview portfolio) {
        final SortedMap<Ratio, SortedMap<Ratio, EnumSet<Rating>>> mostWantedRatings = new TreeMap<>();
        // put the ratings into buckets based on how much we're missing them
        for (Rating r: Rating.values()) {
            final Ratio permittedShare = strategy.getPermittedShare(r);
            if (permittedShare.compareTo(Ratio.ZERO) <= 0) { // prevent division by zero later
                LOGGER.debug("Rating {} permitted share is {} %, skipping.", r, permittedShare.asPercentage());
                continue;
            }
            // under 0 = underinvested, over 0 = overinvested
            final Ratio currentRatingShare = portfolio.getShareOnInvestment(r);
            final Ratio ratio = Ratio.fromRaw(divide(currentRatingShare.bigDecimalValue(),
                    permittedShare.bigDecimalValue()));
            final boolean overinvested = ratio.compareTo(Ratio.ONE) >= 0;
            if (overinvested) { // we over-invested into this rating; do not include
                final BigDecimal percentOver = ratio.asPercentage().subtract(HUNDRED);
                LOGGER.debug("Rating {} over-invested by {} %, skipping. (Expected {}, got {}.)", r, percentOver,
                        permittedShare, currentRatingShare);
                continue;
            }
            LOGGER.debug("Rating {} under-invested by {} %. (Expected {}, got {}.)", r,
                    HUNDRED.subtract(ratio.asPercentage()), permittedShare, currentRatingShare);
            // rank the rating
            mostWantedRatings.computeIfAbsent(ratio, k -> new TreeMap<>(Comparator.reverseOrder()));
            mostWantedRatings.get(ratio).compute(permittedShare, (k, v) -> {
                if (v == null) {
                    return EnumSet.of(r);
                } else {
                    v.add(r);
                    return v;
                }
            });
        }
        return mostWantedRatings.values()
                .stream()
                .flatMap(s -> s.values().stream())
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
