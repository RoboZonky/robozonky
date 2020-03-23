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

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.internal.util.functional.Memoizer;

final class Preferences {

    private static Preferences INSTANCE = null;

    private final PortfolioOverview referencePortfolio;
    private final Set<Rating> ratingRanking;
    private final Supplier<Comparator<Rating>> ratingComparator;
    private final Supplier<Comparator<LoanDescriptor>> primaryMarketplaceComparator;
    private final Supplier<Comparator<ReservationDescriptor>> reservationComparator;
    private final Supplier<Comparator<ParticipationDescriptor>> secondaryMarketplaceComparator;

    private Preferences(PortfolioOverview portfolio, Set<Rating> ratingRanking) {
        this.referencePortfolio = portfolio;
        this.ratingRanking = ratingRanking;
        this.ratingComparator = Memoizer.memoize(() -> Util.getRatingByDemandComparator(ratingRanking));
        this.primaryMarketplaceComparator = Memoizer
            .memoize(() -> new PrimaryMarketplaceComparator(ratingComparator.get()));
        this.reservationComparator = Memoizer.memoize(() -> new ReservationComparator(ratingComparator.get()));
        this.secondaryMarketplaceComparator = Memoizer
            .memoize(() -> new SecondaryMarketplaceComparator(ratingComparator.get()));
    }

    public static synchronized Preferences get(ParsedStrategy strategy, PortfolioOverview portfolio) {
        if (INSTANCE == null) {
            INSTANCE = createInstance(portfolio, Util.rankRatingsByDemand(strategy, portfolio));
            return INSTANCE;
        }
        if (Objects.equals(INSTANCE.referencePortfolio, portfolio)) {
            return INSTANCE;
        }
        Set<Rating> ratingRanking = Util.rankRatingsByDemand(strategy, portfolio);
        if (Objects.equals(INSTANCE.ratingRanking, ratingRanking)) {
            return INSTANCE;
        }
        INSTANCE = createInstance(portfolio, ratingRanking);
        return INSTANCE;
    }

    private static Preferences createInstance(PortfolioOverview portfolio, Set<Rating> ratingRanking) {
        return new Preferences(portfolio, ratingRanking);
    }

    /**
     *
     * @return Iteration order from the most desirable to the least.
     */
    public Set<Rating> getDesirableRatings() {
        return ratingRanking;
    }

    public Comparator<LoanDescriptor> getPrimaryMarketplaceComparator() {
        return primaryMarketplaceComparator.get();
    }

    public Comparator<ReservationDescriptor> getReservationComparator() {
        return reservationComparator.get();
    }

    public Comparator<ParticipationDescriptor> getSecondaryMarketplaceComparator() {
        return secondaryMarketplaceComparator.get();
    }
}
