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

package com.github.robozonky.app.daemon;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.jobs.TenantPayload;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.InstanceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.robozonky.app.events.EventFactory.loanLost;
import static com.github.robozonky.app.events.EventFactory.loanLostLazy;
import static com.github.robozonky.app.events.EventFactory.loanNoLongerDelinquent;
import static com.github.robozonky.app.events.EventFactory.loanNoLongerDelinquentLazy;
import static java.util.stream.Collectors.toList;

/**
 * Updates delinquency information based on the information about loans that are either currently delinquent or no
 * longer active. Will fire events on new delinquencies, defaults and/or loans no longer delinquent.
 */
final class DelinquencyNotificationPayload implements TenantPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelinquencyNotificationPayload.class);
    private static final String DELINQUENT_KEY = "delinquent", DEFAULTED_KEY = "defaulted";

    private static boolean isDefaulted(final Investment i) {
        return i.getPaymentStatus().map(s -> s == PaymentStatus.PAID_OFF).orElse(false);
    }

    private static Stream<String> toIdStrings(final Stream<Investment> investments) {
        return investments.mapToLong(Investment::getId)
                .distinct()
                .sorted()
                .mapToObj(Long::toString);
    }

    private static LongStream toIds(final Stream<String> ids) {
        return ids.mapToLong(Long::parseLong);
    }

    private static Set<Long> toIdSet(final LongStream investments) {
        return investments.boxed().collect(Collectors.toSet());
    }

    private static void processNoLongerDelinquent(final Transactional transactional, final Investment investment,
                                                  final PaymentStatus status) {
        LOGGER.debug("Investment identified as no longer delinquent: {}.", investment);
        switch (status) {
            case WRITTEN_OFF: // investment is lost for good
                transactional.fire(loanLostLazy(() -> {
                    final Loan loan = LoanCache.get().getLoan(investment.getLoanId(), transactional.getTenant());
                    return loanLost(investment, loan);
                }));
                return;
            case PAID:
                LOGGER.debug("Ignoring a repaid investment #{}, will be handled by transaction processors.",
                             investment.getId());
                return;
            default:
                transactional.fire(loanNoLongerDelinquentLazy(() -> {
                    final Loan loan = LoanCache.get().getLoan(investment.getLoanId(), transactional.getTenant());
                    return loanNoLongerDelinquent(investment, loan);
                }));
        }
    }

    static void update(final Transactional transactional, final Collection<Investment> nowDelinquent,
                       final Set<Long> knownDelinquents, final Set<Long> knownDefaulted) {
        LOGGER.debug("Processing delinquent loans by category.");
        // process new defaults
        final Collection<Investment> defaulted = nowDelinquent.stream()
                .filter(DelinquencyNotificationPayload::isDefaulted)
                .filter(i -> !knownDefaulted.contains(i.getId()))
                .collect(toList());
        DelinquencyCategory.DEFAULTED.update(transactional, defaulted);
        // process delinquent investments that are not defaults
        final Collection<Investment> stillDelinquent = nowDelinquent.stream()
                .filter(i -> !isDefaulted(i))
                .collect(toList());
        Stream.of(DelinquencyCategory.values())
                .filter(c -> c != DelinquencyCategory.DEFAULTED)
                .forEach(c -> c.update(transactional, stillDelinquent));
        // process investments that are no longer delinquent
        LOGGER.debug("Processing loans that are no longer delinquent.");
        knownDelinquents.stream()
                .parallel() // there is potentially a lot of those, and there are REST requests involved
                .filter(d -> nowDelinquent.stream().noneMatch(i -> i.getId() == d))
                .distinct()
                .map(d -> transactional.getTenant().call(z -> z.getInvestment(d)))
                .flatMap(i -> i.map(Stream::of).orElse(Stream.empty()))
                .forEach(investment -> investment.getPaymentStatus()
                        .ifPresent(status -> processNoLongerDelinquent(transactional, investment, status)));
    }

    static void notify(final Transactional transactional) {
        LOGGER.debug("Updating delinquent investments.");
        final Tenant tenant = transactional.getTenant();
        final Collection<Investment> currentDelinquents = tenant.call(Zonky::getDelinquentInvestments)
                .parallel() // possibly many pages' worth of results; fetch in parallel
                .collect(toList());
        final int count = currentDelinquents.size();
        LOGGER.debug("There are {} delinquent investments to process.", count);
        final InstanceState<DelinquencyNotificationPayload> transactionalState =
                tenant.getState(DelinquencyNotificationPayload.class);
        final Optional<LongStream> knownDelinquents = transactionalState.getValues(DELINQUENT_KEY)
                .map(DelinquencyNotificationPayload::toIds);
        if (knownDelinquents.isPresent()) { // process updates and notify
            final LongStream defaulted = transactionalState.getValues(DEFAULTED_KEY)
                    .map(DelinquencyNotificationPayload::toIds)
                    .orElse(LongStream.empty());
            update(transactional, currentDelinquents, toIdSet(knownDelinquents.get()), toIdSet(defaulted));
        }
        // store current state
        transactionalState.update(b -> {
            b.put(DELINQUENT_KEY, toIdStrings(currentDelinquents.stream()));
            b.put(DEFAULTED_KEY,
                  toIdStrings(currentDelinquents.stream().filter(DelinquencyNotificationPayload::isDefaulted)));
        });
    }

    @Override
    public void accept(final Tenant tenant) {
        notify(new Transactional(tenant));
    }
}
