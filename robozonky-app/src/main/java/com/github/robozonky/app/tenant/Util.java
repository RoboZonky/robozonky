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

package com.github.robozonky.app.tenant;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.NotFoundException;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.api.remote.Select;
import com.github.robozonky.internal.api.remote.Zonky;
import com.github.robozonky.internal.api.tenant.Tenant;
import com.github.robozonky.internal.util.BigDecimalCalculator;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;

final class Util {

    private static final Logger LOGGER = LogManager.getLogger(Util.class);
    private static final Collector<BigDecimal, ?, BigDecimal> BIGDECIMAL_REDUCING_COLLECTOR =
            reducing(BigDecimal.ZERO, BigDecimalCalculator::plus);

    private Util() {
        // no instances
    }

    static Map<Rating, BigDecimal> getAmountsAtRisk(final Tenant tenant) {
        return tenant.call(Zonky::getDelinquentInvestments)
                .parallel() // possibly many pages' worth of results; fetch in parallel
                .collect(groupingBy(Investment::getRating,
                                    () -> new EnumMap<>(Rating.class),
                                    mapping(i -> {
                                        final BigDecimal principalNotYetReturned = i.getRemainingPrincipal()
                                                .subtract(i.getPaidInterest())
                                                .subtract(i.getPaidPenalty())
                                                .max(BigDecimal.ZERO);
                                        LOGGER.debug("Delinquent: {} CZK in loan #{}, investment #{}.",
                                                     principalNotYetReturned, i.getLoanId(), i.getId());
                                        return principalNotYetReturned;
                                    }, BIGDECIMAL_REDUCING_COLLECTOR)));
    }

    /**
     * @param tenant
     * @return First is sellable with or without fee, second just without.
     */
    static Tuple2<Map<Rating, BigDecimal>, Map<Rating, BigDecimal>> getAmountsSellable(final Tenant tenant) {
        final Select select = new Select()
                .equals("status", "ACTIVE")
                .equalsPlain("onSmp", "CAN_BE_OFFERED_ONLY");
        final Collection<Tuple3<Rating, BigDecimal, BigDecimal>> sellable = tenant.call(z -> z.getInvestments(select))
                .parallel() // possibly many pages' worth of results; fetch in parallel
                .map(i -> Tuple.of(i.getRating(), i.getRemainingPrincipal(), i.getSmpFee().orElse(BigDecimal.ZERO)))
                .collect(Collectors.toList());
        final Map<Rating, BigDecimal> justFeeless = sellable.stream()
                .filter(t -> t._3.signum() == 0)
                .collect(groupingBy(t -> t._1,
                                    () -> new EnumMap<>(Rating.class),
                                    mapping(t -> t._2, BIGDECIMAL_REDUCING_COLLECTOR)));
        final Map<Rating, BigDecimal> all = sellable.stream()
                .collect(groupingBy(t -> t._1,
                                    () -> new EnumMap<>(Rating.class),
                                    mapping(t -> t._2, BIGDECIMAL_REDUCING_COLLECTOR)));
        return Tuple.of(all, justFeeless);
    }

    static Map<Rating, BigDecimal> getAmountsBlocked(final Tenant tenant, final Statistics stats) {
        final long portfolioSize = stats.getCurrentOverview().getPrincipalLeft();
        final Divisor divisor = new Divisor(portfolioSize);
        return tenant.call(Zonky::getBlockedAmounts)
                .parallel()
                .peek(ba -> LOGGER.debug("Found: {}.", ba))
                .filter(ba -> ba.getLoanId() > 0)
                .flatMap(ba -> getLoan(tenant, ba, divisor)
                        .map(loan -> Stream.of(Tuple.of(ba, loan)))
                        .orElse(Stream.empty()))
                .collect(groupingBy(t -> t._2.getRating(),
                                    () -> new EnumMap<>(Rating.class),
                                    mapping(t -> t._1.getAmount(), BIGDECIMAL_REDUCING_COLLECTOR)));
    }

    static Optional<Loan> getLoan(final Tenant tenant, final BlockedAmount ba, final Divisor divisor) {
        final int loanId = ba.getLoanId();
        try {
            return Optional.of(tenant.getLoan(loanId));
        } catch (final NotFoundException ex) {
            /*
             * Zonky has an intermittent caching problem and a failure here would prevent the robot from
             * ever finishing the portfolio update. As a result, the robot would not be able to do
             * anything. Comparatively, being wrong by 0,5 % is not so bad.
             */
            LOGGER.warn("Zonky API mistakenly reports loan #{} as non-existent. " +
                                "Consider reporting this to Zonky so that they can fix their cache.", loanId, ex);
            final BigDecimal amount = ba.getAmount();
            divisor.add(amount.longValue());
            final long shareThatIsWrongPerMille = divisor.getSharePerMille();
            LOGGER.debug("Share per mille: {}.", shareThatIsWrongPerMille);
            if (shareThatIsWrongPerMille >= 5) {
                throw new IllegalStateException("RoboZonky portfolio structure is too far off.", ex);
            } else { // let this slide as the portfolio is only a little bit off
                return Optional.empty();
            }
        }
    }
}
