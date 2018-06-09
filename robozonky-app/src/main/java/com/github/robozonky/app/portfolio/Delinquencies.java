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

package com.github.robozonky.app.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanLostEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.util.BigDecimalCalculator;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;

public class Delinquencies {

    private static final Logger LOGGER = LoggerFactory.getLogger(Delinquencies.class);
    private static final AtomicReference<Map<Rating, BigDecimal>> AMOUNTS_AT_RISK =
            new AtomicReference<>(Collections.emptyMap());
    private static final String DELINQUENT_KEY = "delinquent", DEFAULTED_KEY = "defaulted";

    private Delinquencies() {
        // no need for an instance
    }

    private static Stream<Investment> onlyDefaulted(final Collection<Investment> investments) {
        return investments.stream()
                .filter(i -> i.getPaymentStatus().map(s -> s == PaymentStatus.PAID_OFF).orElse(false));
    }

    private static Stream<String> toIds(final Stream<Investment> investments) {
        return investments.mapToInt(Investment::getId)
                .distinct()
                .sorted()
                .mapToObj(Integer::toString);
    }

    private static IntSet toIdSet(final IntStream investments) {
        return IntHashSet.newSetWith(investments.toArray());
    }

    private static void processNoLongerDelinquent(final Tenant tenant, final int investmentId) {
        final Optional<Investment> inv = tenant.call(z -> z.getInvestment(investmentId));
        if (!inv.isPresent()) {
            LOGGER.warn("Investment not found while it really should have been: #{}.", investmentId);
            return;
        }
        final Investment investment = inv.get();
        if (!investment.getPaymentStatus().isPresent()) {
            LOGGER.warn("Payment status for investment not found while it really should have been: #{}.", investmentId);
            return;
        }
        final Loan loan = tenant.call(z -> LoanCache.INSTANCE.getLoan(investment.getLoanId(), z));
        switch (investment.getPaymentStatus().get()) {
            case WRITTEN_OFF: // investment is lost for good
                Events.fire(new LoanLostEvent(investment, loan));
                return;
            case PAID:
                LOGGER.debug("Ignoring a repaid investment #{}, will be handled by Repayments.", investmentId);
                return;
            default:
                Events.fire(new LoanNoLongerDelinquentEvent(investment, loan));
                return;
        }
    }

    private static void processNoLongerDelinquent(final Tenant tenant, final IntSet investmentIds) {
        investmentIds.forEach(id -> processNoLongerDelinquent(tenant, id));
    }

    private static void processNewDefaults(final Tenant tenant, final Collection<Investment> investments) {
        investments.forEach(i -> {
            final Loan l = tenant.call(z -> LoanCache.INSTANCE.getLoan(i.getLoanId(), z));
            final LoanDefaultedEvent evt = new LoanDefaultedEvent(i, l);
            Events.fire(evt);
        });
    }

    private static void processNonDefaultDelinquents(final Tenant tenant, final Collection<Investment> investments) {
        Stream.of(DelinquencyCategory.values())
                .forEach(c -> c.update(tenant, investments, i -> tenant.call(z -> LoanCache.INSTANCE.getLoan(i, z)),
                                       (l, since) -> getDevelopments(tenant, l, since)));
    }

    static void update(final Tenant tenant, final Collection<Investment> nowDelinquent, final IntSet knownDelinquents,
                       final IntSet knownDefaulted) {
        final Collection<Investment> newDefaults = onlyDefaulted(nowDelinquent)
                .filter(i -> !knownDefaulted.contains(i.getId()))
                .collect(toList());
        processNewDefaults(tenant, newDefaults);
        final Collection<Investment> stillDelinquent = nowDelinquent.stream()
                .filter(i -> !newDefaults.contains(i))
                .filter(i -> !knownDefaulted.contains(i.getId()))
                .collect(toList());
        processNonDefaultDelinquents(tenant, stillDelinquent);
        final IntSet noLongerDelinquent =
                knownDelinquents.reject(d -> nowDelinquent.stream().anyMatch(i -> i.getId() == d));
        processNoLongerDelinquent(tenant, noLongerDelinquent);
    }

    /**
     * Updates delinquency information based on the information about loans that are either currently delinquent or no
     * longer active. Will fire events on new delinquencies and/or on loans no longer delinquent.
     * @param tenant The API that will be used to retrieve the loan instances.
     */
    public static void update(final Tenant tenant) {
        LOGGER.debug("Updating delinquent loans.");
        final Collection<Investment> delinquentInvestments =
                tenant.call(z -> z.getInvestments(new Select()
                                                          .equals("loan.unpaidLastInst", "true")
                                                          .equals("status", "ACTIVE")))
                        .collect(toList());
        final InstanceState<Delinquencies> state = TenantState.of(tenant.getSessionInfo())
                .in(Delinquencies.class);
        final Optional<IntStream> delinquents = state.getValues(DELINQUENT_KEY)
                .map(s -> s.mapToInt(Integer::parseInt));
        if (delinquents.isPresent()) { // process updates and notify
            final IntStream defaulted = state.getValues(DEFAULTED_KEY)
                    .map(s -> s.mapToInt(Integer::parseInt))
                    .orElse(IntStream.empty());
            update(tenant, delinquentInvestments, toIdSet(delinquents.get()), toIdSet(defaulted));
        }
        // store current state
        state.update(b -> {
            b.put(DELINQUENT_KEY, toIds(delinquentInvestments.stream()));
            b.put(DEFAULTED_KEY, toIds(onlyDefaulted(delinquentInvestments)));
        });
        // update amounts at risk
        final Map<Rating, BigDecimal> atRisk = delinquentInvestments.stream()
                .collect(groupingBy(Investment::getRating,
                                    () -> new EnumMap<>(Rating.class),
                                    mapping(i -> {
                                        final BigDecimal principalNotYetReturned = i.getRemainingPrincipal()
                                                .subtract(i.getPaidInterest())
                                                .subtract(i.getPaidPenalty());
                                        LOGGER.debug("Delinquent: {} CZK in loan #{}, investment #{}.",
                                                     principalNotYetReturned, i.getLoanId(), i.getId());
                                        return principalNotYetReturned.max(BigDecimal.ZERO);
                                    }, reducing(BigDecimal.ZERO, BigDecimalCalculator::plus))));
        AMOUNTS_AT_RISK.set(atRisk);
        LOGGER.debug("Done, new amounts at risk are {}.", atRisk);
    }

    static Map<Rating, BigDecimal> getAmountsAtRisk() {
        return Delinquencies.AMOUNTS_AT_RISK.get();
    }

    private static List<Development> getDevelopments(final Tenant auth, final Loan loan,
                                                     final LocalDate delinquentSince) {
        final List<Development> developments = auth.call(z -> z.getDevelopments(loan))
                .filter(d -> d.getDateFrom().toLocalDate().isAfter(delinquentSince.minusDays(1)))
                .collect(toList());
        Collections.reverse(developments);
        return developments;
    }
}
