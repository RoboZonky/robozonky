/*
 * Copyright 2020 The RoboZonky Project
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

import static com.github.robozonky.app.events.impl.EventFactory.loanLost;
import static com.github.robozonky.app.events.impl.EventFactory.loanLostLazy;
import static com.github.robozonky.app.events.impl.EventFactory.loanNoLongerDelinquent;
import static com.github.robozonky.app.events.impl.EventFactory.loanNoLongerDelinquentLazy;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Label;
import com.github.robozonky.api.remote.enums.SellStatus;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.app.events.SessionEvents;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;

/**
 * Updates delinquency information based on the information about loans that are either currently delinquent or no
 * longer active. Will fire events on new delinquencies, defaults and/or loans no longer delinquent.
 */
final class DelinquencyNotificationPayload implements TenantPayload {

    private static final Logger LOGGER = LogManager.getLogger(DelinquencyNotificationPayload.class);

    private final Function<Tenant, Registry> registryFunction;
    private final boolean force;

    public DelinquencyNotificationPayload() {
        this(Registry::new, false);
    }

    DelinquencyNotificationPayload(final Function<Tenant, Registry> registryFunction, final boolean force) {
        this.registryFunction = registryFunction;
        this.force = force;
    }

    private static boolean isDefaulted(final Investment i) {
        var isDue = i.getLoan()
            .getHealthStats()
            .getCurrentDaysDue() > 0;
        var isTerminated = i.getLoan()
            .getLabel()
            .map(label -> label == Label.TERMINATED)
            .orElse(false);
        return isDue && isTerminated;
    }

    private static void processNoLongerDelinquent(final Registry registry, final Investment investment,
            final PowerTenant tenant) {
        registry.remove(investment);
        LOGGER.debug("Investment identified as no longer delinquent: {}.", investment);
        if (investment.getLoan()
            .getPayments()
            .getUnpaid() == 0 &&
                investment.getPrincipal()
                    .getUnpaid()
                    .isZero()) {
            LOGGER.debug("Ignoring a repaid investment #{}, will be handled elsewhere.",
                    investment.getId());
            return;
        } else if (investment.getSellStatus() == SellStatus.NOT_SELLABLE) { // Investment is lost for good.
            // TODO Try to convince Zonky to add a dedicated status for this eventuality.
            tenant.fire(loanLostLazy(() -> {
                final Loan loan = tenant.getLoan(investment.getLoan()
                    .getId());
                return loanLost(investment, loan);
            }));
            return;
        }
        tenant.fire(loanNoLongerDelinquentLazy(() -> {
            final Loan loan = tenant.getLoan(investment.getLoan()
                .getId());
            return loanNoLongerDelinquent(investment, loan);
        }));
    }

    private static void processDelinquent(final PowerTenant tenant, final Registry registry,
            final Investment currentDelinquent) {
        final long investmentId = currentDelinquent.getId();
        final EnumSet<Category> knownCategories = registry.getCategories(currentDelinquent);
        if (knownCategories.contains(Category.HOPELESS)) {
            LOGGER.debug("Investment #{} may not be promoted anymore.", investmentId);
            return;
        }
        final int daysPastDue = currentDelinquent.getLoan()
            .getHealthStats()
            .getCurrentDaysDue();
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
        return investments.parallelStream()
            .filter(DelinquencyNotificationPayload::isDefaulted);
    }

    private static Stream<Investment> getNonDefaulted(final Set<Investment> investments) {
        return investments.parallelStream()
            .filter(i -> i.getLoan()
                .getHealthStats()
                .getCurrentDaysDue() > 0)
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
                    processNoLongerDelinquent(registry, i, tenant);
                });
            // potentially thousands of items, with relatively heavy logic behind them
            getDefaulted(delinquents).forEach(d -> processDefaulted(tenant, registry, d));
            getNonDefaulted(delinquents).forEach(d -> processDelinquent(tenant, registry, d));
        } else {
            getDefaulted(delinquents).forEach(d -> registry.addCategory(d, Category.DEFAULTED));
            getNonDefaulted(delinquents).forEach(d -> {
                for (final Category cat : Category.values()) {
                    int dpd = d.getLoan()
                        .getHealthStats()
                        .getCurrentDaysDue();
                    if (cat.getThresholdInDays() > dpd || cat.getThresholdInDays() < 0) {
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
        PowerTenant powerTenant = (PowerTenant) tenant;
        SessionEvents sessionEvents = Events.forSession(powerTenant);
        boolean shouldTrigger = force || Stream.of(LoanDefaultedEvent.class, LoanDelinquent10DaysOrMoreEvent.class,
                LoanDelinquent30DaysOrMoreEvent.class, LoanDelinquent60DaysOrMoreEvent.class,
                LoanDelinquent90DaysOrMoreEvent.class, LoanNowDelinquentEvent.class,
                LoanNoLongerDelinquentEvent.class)
            .anyMatch(sessionEvents::isListenerRegistered);
        if (!shouldTrigger) {
            LOGGER.debug("Skipping on account of no event listener being configured to receive the results.");
            return;
        }
        powerTenant.inTransaction(this::process);
    }
}
