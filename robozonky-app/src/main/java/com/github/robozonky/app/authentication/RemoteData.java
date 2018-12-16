/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.authentication;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.NotFoundException;

import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.daemon.LoanCache;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.util.BigDecimalCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;

class RemoteData {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteData.class);

    private final Wallet wallet;
    private final Statistics statistics;
    private final Map<Integer, Blocked> blocked;
    private final Map<Rating, BigDecimal> atRisk;

    private RemoteData(final Wallet wallet, final Statistics statistics, final Map<Integer, Blocked> blocked,
                       final Map<Rating, BigDecimal> atRisk) {
        this.wallet = wallet;
        this.statistics = statistics;
        this.blocked = blocked;
        this.atRisk = atRisk;
    }

    public static RemoteData load(final Tenant tenant) {
        LOGGER.debug("Loading the latest Zonky portfolio information.");
        final Wallet wallet = tenant.call(Zonky::getWallet);
        final Statistics statistics = tenant.call(Zonky::getStatistics);
        final Map<Integer, Blocked> blocked = readBlockedAmounts(tenant, statistics);
        final Map<Rating, BigDecimal> atRisk = getAmountsAtRisk(tenant);
        LOGGER.debug("Finished.");
        return new RemoteData(wallet, statistics, blocked, atRisk);
    }

    private static Map<Rating, BigDecimal> getAmountsAtRisk(final Tenant tenant) {
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

    private static Map<Integer, Blocked> readBlockedAmounts(final Tenant tenant, final Statistics stats) {
        final long portfolioSize = stats.getCurrentOverview().getPrincipalLeft();
        final Divisor divisor = new Divisor(portfolioSize);
        return tenant.call(Zonky::getBlockedAmounts)
                .parallel()
                .peek(ba -> LOGGER.debug("Found: {}.", ba))
                .filter(ba -> ba.getLoanId() > 0)
                .flatMap(ba -> {
                    final int loanId = ba.getLoanId();
                    try {
                        final Loan l = LoanCache.get().getLoan(loanId, tenant);
                        return Stream.of(new Blocked(ba, l.getRating()));
                    } catch (final NotFoundException ex) {
                        /*
                         * Zonky has an intermittent caching problem and a failure here would prevent the robot from
                         * ever finishing the portfolio update. As a result, the robot would not be able to do
                         * anything. Comparatively, being wrong by 0,5 % is not so bad.
                         */
                        LOGGER.warn("Zonky API mistakenly reports loan #{} as non-existent. " +
                                            "Consider reporting this to Zonky so that they can fix it.",
                                    loanId, ex);
                        final BigDecimal amount = ba.getAmount();
                        divisor.add(amount.longValue());
                        final long shareThatIsWrongPerMille = divisor.getSharePerMille();
                        if (shareThatIsWrongPerMille >= 5) {
                            throw new IllegalStateException("RoboZonky portfolio structure is too far off.", ex);
                        } else { // let this slide as the portfolio is only a little bit off
                            return Stream.empty();
                        }
                    }
                })
                .collect(Collectors.toMap(Blocked::getId, b -> b));
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public Map<Integer, Blocked> getBlocked() {
        return Collections.unmodifiableMap(blocked);
    }

    public Map<Rating, BigDecimal> atRisk() {
        return Collections.unmodifiableMap(atRisk);
    }
}
