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

import java.util.Collection;
import java.util.stream.Stream;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedReservation;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationMode;
import com.github.robozonky.api.strategies.ReservationStrategy;

import static com.github.robozonky.strategy.natural.Audit.LOGGER;

class NaturalLanguageReservationStrategy implements ReservationStrategy {

    private final ParsedStrategy strategy;

    public NaturalLanguageReservationStrategy(final ParsedStrategy p) {
        this.strategy = p;
    }

    @Override
    public ReservationMode getMode() {
        return strategy.getReservationMode()
                .orElseThrow(() -> new IllegalStateException("Reservations are not enabled, yet strategy exists."));
    }

    @Override
    public Stream<RecommendedReservation> recommend(final Collection<ReservationDescriptor> available,
                                                    final PortfolioOverview portfolio,
                                                    final Restrictions restrictions) {
        if (!Util.isAcceptable(strategy, portfolio)) {
            return Stream.empty();
        }
        var preferences = Preferences.get(strategy, portfolio);
        var withoutUndesirable = available.parallelStream()
                .filter(d -> { // skip loans in ratings which are not required by the strategy
                    boolean isAcceptable = preferences.getDesirableRatings().contains(d.item().getRating());
                    if (!isAcceptable) {
                        LOGGER.debug("{} skipped due to an undesirable rating.", d.item());
                    }
                    return isAcceptable;
                });
        return strategy.getApplicableReservations(withoutUndesirable, portfolio)
                .sorted(preferences.getReservationComparator())
                .flatMap(d -> { // recommend amount to invest per strategy
                    final Money amount = d.item().getMyReservation().getReservedAmount();
                    return d.recommend(amount).stream();
                });
    }
}
