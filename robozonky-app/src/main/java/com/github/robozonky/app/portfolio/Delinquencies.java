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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanLostEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.TransactionalPortfolio;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.internal.util.BigDecimalCalculator;
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

    private static boolean isDefaulted(final Investment i) {
        return i.getPaymentStatus().map(s -> s == PaymentStatus.PAID_OFF).orElse(false);
    }

    private static Stream<String> toIds(final Stream<Investment> investments) {
        return investments.mapToInt(Investment::getId)
                .distinct()
                .sorted()
                .mapToObj(Integer::toString);
    }

    private static Set<Integer> toIdSet(final IntStream investments) {
        return investments.boxed().collect(Collectors.toSet());
    }

    private static void processNoLongerDelinquent(final TransactionalPortfolio transactionalPortfolio,
                                                  final Investment investment, final PaymentStatus status) {
        final Loan loan = getLoan(transactionalPortfolio, investment);
        switch (status) {
            case WRITTEN_OFF: // investment is lost for good
                transactionalPortfolio.fire(new LoanLostEvent(investment, loan));
                return;
            case PAID:
                LOGGER.debug("Ignoring a repaid investment #{}, will be handled by Repayments.",
                             investment.getId());
                return;
            default:
                transactionalPortfolio.fire(new LoanNoLongerDelinquentEvent(investment, loan));
                return;
        }
    }

    private static void processNonDefaultDelinquents(final TransactionalPortfolio transactionalPortfolio,
                                                     final Collection<Investment> investments) {
        Stream.of(DelinquencyCategory.values())
                .forEach(c -> c.update(transactionalPortfolio, investments));
    }

    private static Loan getLoan(final TransactionalPortfolio portfolio, final Investment investment) {
        return portfolio.getTenant().call(z -> LoanCache.INSTANCE.getLoan(investment, z));
    }

    static void update(final TransactionalPortfolio transactionalPortfolio, final Collection<Investment> nowDelinquent,
                       final Set<Integer> knownDelinquents, final Set<Integer> knownDefaulted) {
        // process new defaults
        nowDelinquent.stream()
                .filter(Delinquencies::isDefaulted)
                .filter(i -> !knownDefaulted.contains(i.getId()))
                .forEach(i -> {
                    final Loan l = getLoan(transactionalPortfolio, i);
                    final LoanDefaultedEvent evt = new LoanDefaultedEvent(i, l);
                    transactionalPortfolio.fire(evt);
                });
        // process delinquent investments that are not defaults
        final Collection<Investment> stillDelinquent = nowDelinquent.stream()
                .filter(i -> !isDefaulted(i))
                .collect(toList());
        processNonDefaultDelinquents(transactionalPortfolio, stillDelinquent);
        // process investments that are no longer delinquent
        knownDelinquents.stream()
                .filter(d -> nowDelinquent.stream().noneMatch(i -> i.getId() == d))
                .distinct()
                .map(d -> transactionalPortfolio.getTenant().call(z -> z.getInvestment(d)))
                .flatMap(i -> i.map(Stream::of).orElse(Stream.empty()))
                .forEach(investment -> {
                    LOGGER.debug("Investment identified as no longer delinquent: {}.", investment);
                    investment.getPaymentStatus()
                            .ifPresent(status -> processNoLongerDelinquent(transactionalPortfolio, investment, status));
                });
    }

    /**
     * Updates delinquency information based on the information about loans that are either currently delinquent or no
     * longer active. Will fire events on new delinquencies and/or on loans no longer delinquent.
     * @param transactionalPortfolio The API that will be used to retrieve the loan instances.
     */
    public static void update(final TransactionalPortfolio transactionalPortfolio) {
        LOGGER.debug("Updating delinquent loans.");
        final Tenant tenant = transactionalPortfolio.getTenant();
        final Collection<Investment> delinquentInvestments =
                tenant.call(z -> z.getInvestments(new Select()
                                                          .equals("loan.unpaidLastInst", "true")
                                                          .equals("status", "ACTIVE")))
                        .collect(toList());
        final InstanceState<Delinquencies> transactionalState = tenant.getState(Delinquencies.class);
        final Optional<IntStream> delinquents = transactionalState.getValues(DELINQUENT_KEY)
                .map(s -> s.mapToInt(Integer::parseInt));
        if (delinquents.isPresent()) { // process updates and notify
            final IntStream defaulted = transactionalState.getValues(DEFAULTED_KEY)
                    .map(s -> s.mapToInt(Integer::parseInt))
                    .orElse(IntStream.empty());
            update(transactionalPortfolio, delinquentInvestments, toIdSet(delinquents.get()), toIdSet(defaulted));
        }
        // store current state
        transactionalState.update(b -> {
            b.put(DELINQUENT_KEY, toIds(delinquentInvestments.stream()));
            b.put(DEFAULTED_KEY, toIds(delinquentInvestments.stream().filter(Delinquencies::isDefaulted)));
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
}
