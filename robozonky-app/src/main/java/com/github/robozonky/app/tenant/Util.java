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
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.NotFoundException;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.util.BigDecimalCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;

final class Util {

    private static final Logger LOGGER = LogManager.getLogger(Util.class);

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
                                    }, reducing(BigDecimal.ZERO, BigDecimalCalculator::plus))));
    }

    static Map<Integer, Blocked> readBlockedAmounts(final Tenant tenant, final Statistics stats) {
        final long portfolioSize = stats.getCurrentOverview().getPrincipalLeft();
        final Divisor divisor = new Divisor(portfolioSize);
        return tenant.call(Zonky::getBlockedAmounts)
                .parallel()
                .peek(ba -> LOGGER.debug("Found: {}.", ba))
                .filter(ba -> ba.getLoanId() > 0)
                .flatMap(ba -> getLoan(tenant, ba, divisor)
                        .map(l -> Stream.of(new Blocked(ba, l.getRating())))
                        .orElse(Stream.empty()))
                .collect(Collectors.toMap(Blocked::getId, b -> b, Util::merge));
    }

    /**
     * Two blocked amounts related to the same loan. May happen if robot invests while the user manually invests through
     * the reservation system as well - that's the only time two investments in one {@link Loan} are possible.
     * @param a
     * @param b
     * @return Sum of both amounts, in the rating that both of the amounts share.
     * @throws IllegalArgumentException When the ratings of the two amounts don't match. That represents an invalid
     * situation.
     */
    private static Blocked merge(final Blocked a, final Blocked b) {
        final Rating rating = a.getRating();
        if (rating != b.getRating()) {
            throw new IllegalArgumentException("Ratings do not match: " + a + " and " + b);
        }
        final BigDecimal total = a.getAmount().add(b.getAmount());
        return new Blocked(total, rating, a.isPersistent());
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
