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

package com.github.robozonky.app.delinquencies;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.app.tenant.TransactionalPowerTenant;
import com.github.robozonky.common.jobs.TenantPayload;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.robozonky.app.events.impl.EventFactory.loanLost;
import static com.github.robozonky.app.events.impl.EventFactory.loanLostLazy;
import static com.github.robozonky.app.events.impl.EventFactory.loanNoLongerDelinquent;
import static com.github.robozonky.app.events.impl.EventFactory.loanNoLongerDelinquentLazy;

/**
 * Updates delinquency information based on the information about loans that are either currently delinquent or no
 * longer active. Will fire events on new delinquencies, defaults and/or loans no longer delinquent.
 */
final class DelinquencyNotificationPayload implements TenantPayload {

    private static final Logger LOGGER = LogManager.getLogger(DelinquencyNotificationPayload.class);

    private final Function<Tenant, Registry> registryFunction;

    public DelinquencyNotificationPayload() {
        this(Registry::new);
    }

    DelinquencyNotificationPayload(final Function<Tenant, Registry> registryFunction) {
        this.registryFunction = registryFunction;
    }

    private static boolean isDefaulted(final Investment i) {
        return i.getPaymentStatus().map(s -> s == PaymentStatus.PAID_OFF).orElse(false);
    }

    private static void processNoLongerDelinquent(final PowerTenant tenant, final Investment investment,
                                                  final PaymentStatus status) {
        LOGGER.debug("Investment identified as no longer delinquent: {}.", investment);
        switch (status) {
            case WRITTEN_OFF: // investment is lost for good
                tenant.fire(loanLostLazy(() -> {
                    final Loan loan = tenant.getLoan(investment.getLoanId());
                    return loanLost(investment, loan);
                }));
                return;
            case PAID:
                LOGGER.debug("Ignoring a repaid investment #{}, will be handled by transaction processors.",
                             investment.getId());
                return;
            default:
                tenant.fire(loanNoLongerDelinquentLazy(() -> {
                    final Loan loan = tenant.getLoan(investment.getLoanId());
                    return loanNoLongerDelinquent(investment, loan);
                }));
        }
    }

    private static void processNoLongerDelinquent(final Investment investment, final PowerTenant tenant) {
        investment.getPaymentStatus().ifPresent(status -> processNoLongerDelinquent(tenant, investment, status));
    }

    private static void processDelinquent(final PowerTenant tenant, final Registry registry,
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
                .filter(c -> c.getThresholdInDays() >= 0) // ignore the DEFAULTED category, which gets special treatment
                .filter(c -> c.getThresholdInDays() <= daysPastDue)
                .max(Comparator.comparing(Category::getThresholdInDays));
        if (firstNextCategory.isPresent()) {
            final Category category = firstNextCategory.get();
            LOGGER.debug("Investment #{} placed to category {}.", investmentId, category);
            category.process(tenant, currentDelinquent);
            registry.addCategory(currentDelinquent, category);
        } else {
            LOGGER.debug("Investment #{} can not yet be promoted to the next category.", investmentId);
        }
    }

    private static void processDefaulted(final PowerTenant tenant, final Registry registry,
                                         final Investment currentDelinquent) {
        final long investmentId = currentDelinquent.getId();
        final EnumSet<Category> knownCategories = registry.getCategories(currentDelinquent);
        if (knownCategories.contains(Category.DEFAULTED)) {
            LOGGER.debug("Investment #{} already tracked as defaulted.", investmentId);
        } else {
            final Category category = Category.DEFAULTED;
            LOGGER.debug("Investment #{} defaulted.", investmentId);
            category.process(tenant, currentDelinquent);
            registry.addCategory(currentDelinquent, category);
        }
    }

    private static Stream<Investment> getDefaulted(final Set<Investment> investments) {
        return investments.parallelStream().filter(DelinquencyNotificationPayload::isDefaulted);
    }

    private static Stream<Investment> getNonDefaulted(final Set<Investment> investments) {
        return investments.parallelStream()
                .filter(i -> i.getDaysPastDue() > 0)
                .filter(i -> !isDefaulted(i));
    }

    private void process(final PowerTenant tenant) {
        final Set<Investment> delinquents = tenant.call(Zonky::getDelinquentInvestments)
                .parallel() // possibly many pages' worth of results; fetch in parallel
                .collect(Collectors.toSet());
        final int count = delinquents.size();
        LOGGER.debug("There are {} delinquent investments to process.", count);
        final Registry registry = registryFunction.apply(tenant);
        if (registry.isInitialized()) {
            registry.complement(delinquents)
                    .parallelStream()
                    .forEach(i -> {
                        registry.remove(i);
                        processNoLongerDelinquent(i, tenant);
                    });
            // potentially thousands of items, with relatively heavy logic behind them
            getDefaulted(delinquents).forEach(d -> processDefaulted(tenant, registry, d));
            getNonDefaulted(delinquents).forEach(d -> processDelinquent(tenant, registry, d));
        } else {
            getDefaulted(delinquents).forEach(d -> registry.addCategory(d, Category.DEFAULTED));
            getNonDefaulted(delinquents).forEach(d -> {
                for (final Category cat : Category.values()) {
                    if (cat.getThresholdInDays() > d.getDaysPastDue() || cat.getThresholdInDays() < 0) {
                        continue;
                    }
                    registry.addCategory(d, cat);
                }
                LOGGER.debug("No category found for investment #{}.", d.getId());
            });
        }
        registry.persist();
    }

    @Override
    public void accept(final Tenant tenant) {
        final TransactionalPowerTenant transactionalTenant = PowerTenant.transactional((PowerTenant) tenant);
        try {
            process(transactionalTenant);
            transactionalTenant.commit();
        } catch (final Exception ex) {
            LOGGER.debug("Aborting transaction.", ex);
            transactionalTenant.abort();
        }
    }
}
