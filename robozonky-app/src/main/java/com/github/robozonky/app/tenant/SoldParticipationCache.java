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

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.common.async.Reloadable;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.tenant.Tenant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SoldParticipationCache {

    private static final Logger LOGGER = LogManager.getLogger(SoldParticipationCache.class);
    private static final Map<SessionInfo, SoldParticipationCache> INSTANCES = new WeakHashMap<>(0);

    private final Set<Integer> listedSoldLocally = new CopyOnWriteArraySet<>();
    private final Reloadable<Set<Integer>> listedSoldRemotely;

    private SoldParticipationCache(final Tenant tenant) {
        this.listedSoldRemotely = Reloadable.with(() -> retrieveSoldParticipationIds(tenant))
                .reloadAfter(Duration.ofMinutes(5))
                .build();
    }

    private static Set<Integer> retrieveSoldParticipationIds(final Tenant tenant) {
        final Select s = new Select().equals("status", "SOLD");
        return tenant.call(zonky -> zonky.getInvestments(s))
                .mapToInt(Investment::getLoanId)
                .distinct()
                .boxed()
                .collect(Collectors.toSet());
    }

    private static SoldParticipationCache newCache(final Tenant tenant) {
        return new SoldParticipationCache(tenant);
    }

    public static SoldParticipationCache forTenant(final Tenant tenant) {
        return INSTANCES.computeIfAbsent(tenant.getSessionInfo(), key -> newCache(tenant));
    }

    /**
     * For testing purposes only.
     */
    static void resetAll() {
        INSTANCES.clear();
    }

    public void markAsSold(final int loanId) {
        listedSoldLocally.add(loanId);
    }

    public boolean wasOnceSold(final int loanId) {
        return listedSoldLocally.contains(loanId) ||
                listedSoldRemotely.get().map(s -> s.contains(loanId)).getOrElseGet(ex -> {
                    LOGGER.info("Failed retrieving sold loans from Zonky.", ex);
                    return false;
                });
    }
}
