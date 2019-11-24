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

package com.github.robozonky.app.summaries;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collector;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;

final class Util {

    private static final Logger LOGGER = LogManager.getLogger(Util.class);
    private static final Collector<Money, ?, Money> REDUCING_COLLECTOR = reducing(Money.ZERO, Money::add);

    private Util() {
        // no instances
    }

    static Map<Rating, Money> getAmountsAtRisk(final Tenant tenant) {
        return tenant.call(Zonky::getDelinquentInvestments)
                .parallel() // possibly many pages' worth of results; fetch in parallel
                .collect(groupingBy(Investment::getRating,
                                    () -> new EnumMap<>(Rating.class),
                                    mapping(i -> {
                                        final Money remaining = i.getRemainingPrincipal().orElseThrow();
                                        final Money principalNotYetReturned = remaining.subtract(i.getPaidInterest())
                                                .subtract(i.getPaidPenalty())
                                                .max(remaining.getZero());
                                        LOGGER.debug("Delinquent: {} in loan #{}, investment #{}.",
                                                     principalNotYetReturned, i.getLoanId(), i.getId());
                                        return principalNotYetReturned;
                                    }, REDUCING_COLLECTOR)));
    }

    /**
     * @param tenant
     * @return First is sellable with or without fee, second just without.
     */
    static Tuple2<Map<Rating, Money>, Map<Rating, Money>> getAmountsSellable(final Tenant tenant) {
        final Select select = Select.sellableParticipations();
        final Collection<Tuple3<Rating, Money, Money>> sellable = tenant.call(z -> z.getInvestments(select))
                .parallel() // possibly many pages' worth of results; fetch in parallel
                .map(i -> Tuple.of(i.getRating(), i.getRemainingPrincipal().orElseThrow(), i.getSmpFee().orElse(Money.ZERO)))
                .collect(toList());
        final Map<Rating, Money> justFeeless = sellable.stream()
                .filter(t -> t._3.isZero())
                .collect(groupingBy(t -> t._1,
                                    () -> new EnumMap<>(Rating.class),
                                    mapping(t -> t._2, REDUCING_COLLECTOR)));
        final Map<Rating, Money> all = sellable.stream()
                .collect(groupingBy(t -> t._1,
                                    () -> new EnumMap<>(Rating.class),
                                    mapping(t -> t._2, REDUCING_COLLECTOR)));
        return Tuple.of(all, justFeeless);
    }

}
