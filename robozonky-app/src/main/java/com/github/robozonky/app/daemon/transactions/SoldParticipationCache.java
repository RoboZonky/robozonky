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

package com.github.robozonky.app.daemon.transactions;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SoldParticipationCache {

    private static final Map<SessionInfo, SoldParticipationCache> INSTANCES = new WeakHashMap<>(0);
    private static final Logger LOGGER = LoggerFactory.getLogger(SoldParticipationCache.class);

    private final Supplier<Set<Integer>> refresher;
    private final Set<Integer> listedSoldLocally = new HashSet<>(0);
    private volatile Instant lastSuccessfulRefresh = Instant.EPOCH;
    private volatile Set<Integer> listedSoldRemotely = Collections.emptySet();

    private SoldParticipationCache(final Supplier<Set<Integer>> refresher) {
        this.refresher = refresher;
    }

    private static Set<Integer> getSoldLoans(final Tenant tenant) {
        final Select s = new Select().equals("status", "SOLD");
        return tenant.call(zonky -> zonky.getInvestments(s))
                .mapToInt(Investment::getLoanId)
                .distinct()
                .boxed()
                .collect(Collectors.toSet());
    }

    private static SoldParticipationCache newCache(final Tenant tenant) {
        return new SoldParticipationCache(() -> getSoldLoans(tenant));
    }

    public static SoldParticipationCache forTenant(final Tenant tenant) {
        return INSTANCES.computeIfAbsent(tenant.getSessionInfo(), key -> newCache(tenant));
    }

    synchronized void markAsSold(final int loanId) {
        listedSoldLocally.add(loanId);
    }

    private void refresh() {
        try {
            listedSoldRemotely = refresher.get();
            lastSuccessfulRefresh = Instant.now();
        } catch (final Exception ex) {
            LOGGER.info("Failed fetching the list of sold loans from Zonky. Using last known one.", ex);
        }
    }

    public synchronized boolean wasOnceSold(final int loanId) {
        if (Instant.now().minus(Duration.ofMinutes(5)).isAfter(lastSuccessfulRefresh)) {
            refresh();
        }
        return listedSoldLocally.contains(loanId) || listedSoldRemotely.contains(loanId);
    }
}
