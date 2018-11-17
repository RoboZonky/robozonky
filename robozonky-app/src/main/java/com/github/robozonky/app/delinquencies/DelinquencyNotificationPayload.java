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

package com.github.robozonky.app.delinquencies;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.app.daemon.LoanCache;
import com.github.robozonky.app.daemon.Transactional;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.jobs.TenantPayload;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.robozonky.app.events.EventFactory.loanLost;
import static com.github.robozonky.app.events.EventFactory.loanLostLazy;
import static com.github.robozonky.app.events.EventFactory.loanNoLongerDelinquent;
import static com.github.robozonky.app.events.EventFactory.loanNoLongerDelinquentLazy;
import static java.util.stream.Collectors.toMap;

/**
 * Updates delinquency information based on the information about loans that are either currently delinquent or no
 * longer active. Will fire events on new delinquencies, defaults and/or loans no longer delinquent.
 */
final class DelinquencyNotificationPayload implements TenantPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelinquencyNotificationPayload.class);

    private static boolean isDefaulted(final Investment i) {
        return i.getPaymentStatus().map(s -> s == PaymentStatus.PAID_OFF).orElse(false);
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

    private static void processNoLongerDelinquent(final Investment investment, final Transactional transactional) {
        investment.getPaymentStatus().ifPresent(status -> processNoLongerDelinquent(transactional, investment, status));
    }

    private static void processDelinquent(final Transactional transactional, final Registry registry,
                                          final Investment currentDelinquent) {
        final long investmentId = currentDelinquent.getId();
        final EnumSet<Category> knownCategories = registry.getCategories(currentDelinquent);
        if (knownCategories.contains(Category.HOPELESS)) {
            LOGGER.debug("Investment #{} may not be promoted anymore.", investmentId);
            return;
        }
        final int daysPastDue = currentDelinquent.getDaysPastDue();
        final EnumSet<Category> unusedCategories = EnumSet.complementOf(knownCategories);
        final Optional<Category> firstNextCategory = unusedCategories.stream()
                .filter(c -> c.getThresholdInDays() >= daysPastDue)
                .findFirst();
        if (firstNextCategory.isPresent()) {
            final Category category = firstNextCategory.get();
            LOGGER.debug("Investment #{} placed to category {}.", investmentId, category);
            category.process(transactional, currentDelinquent);
            registry.addCategory(currentDelinquent, category);
        } else {
            LOGGER.debug("Investment #{} can not yet be promoted to the next category.", investmentId);
        }
    }

    private static void processDefaulted(final Transactional transactional, final Registry registry,
                                         final Investment currentDelinquent) {
        final long investmentId = currentDelinquent.getId();
        final EnumSet<Category> knownCategories = registry.getCategories(currentDelinquent);
        if (knownCategories.contains(Category.DEFAULTED)) {
            LOGGER.debug("Investment #{} already tracked as defaulted.", investmentId);
        } else {
            final Category category = Category.DEFAULTED;
            LOGGER.debug("Investment #{} placed to category {}.", investmentId, category);
            category.process(transactional, currentDelinquent);
            registry.addCategory(currentDelinquent, category);
        }
    }

    private static void process(final Transactional transactional) {
        final Tenant tenant = transactional.getTenant();
        final Map<Long, Investment> currentDelinquents = tenant.call(Zonky::getDelinquentInvestments)
                .parallel() // possibly many pages' worth of results; fetch in parallel
                .collect(toMap(Investment::getId, i -> i));
        final int count = currentDelinquents.size();
        LOGGER.debug("There are {} delinquent investments to process.", count);
        final Registry registry = new Registry(tenant);
        if (registry.isInitialized()) {
            registry.complement(currentDelinquents.values())
                    .parallelStream()
                    .forEach(i -> {
                        registry.remove(i);
                        processNoLongerDelinquent(i, transactional);
                    });
            currentDelinquents.values().parallelStream()
                    .filter(DelinquencyNotificationPayload::isDefaulted)
                    .forEach(currentDelinquent -> processDefaulted(transactional, registry, currentDelinquent));
            currentDelinquents.values().parallelStream()
                    .filter(i -> !isDefaulted(i))
                    .forEach(currentDelinquent -> processDelinquent(transactional, registry, currentDelinquent));
        } else {
            currentDelinquents.values().parallelStream()
                    .filter(DelinquencyNotificationPayload::isDefaulted)
                    .forEach(currentDelinquent -> registry.addCategory(currentDelinquent, Category.DEFAULTED));
            currentDelinquents.values().parallelStream()
                    .filter(i -> !isDefaulted(i))
                    .forEach(currentDelinquent -> {
                        final int dayPastDue = currentDelinquent.getDaysPastDue();
                        for (final Category cat: Category.values()) {
                            if (cat.getThresholdInDays() < dayPastDue) {
                                continue;
                            }
                            registry.addCategory(currentDelinquent, Category.DEFAULTED);
                        }
                        LOGGER.debug("No category found for investment #{}.", currentDelinquent.getId());
                    });
        }
        registry.persist();
        transactional.run();
    }

    @Override
    public void accept(final Tenant tenant) {
        final Transactional transactional = new Transactional(tenant);
        process(transactional);
        transactional.run();
    }
}
