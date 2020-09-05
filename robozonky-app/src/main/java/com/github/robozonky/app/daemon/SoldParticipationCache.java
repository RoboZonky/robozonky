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

package com.github.robozonky.app.daemon;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.internal.async.Reloadable;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.state.InstanceState;
import com.github.robozonky.internal.tenant.Tenant;

final class SoldParticipationCache {

    private static final Logger LOGGER = LogManager.getLogger(SoldParticipationCache.class);
    private static final Map<SessionInfo, SoldParticipationCache> INSTANCES = new WeakHashMap<>(0);
    private static final String KEY = "offeredButNotYetSeenSold";

    private final InstanceState<SoldParticipationCache> state;
    private final Set<Long> listedSoldLocally = new CopyOnWriteArraySet<>();
    private final Reloadable<Set<Long>> listedSoldRemotely;

    private SoldParticipationCache(final Tenant tenant) {
        this.state = tenant.getState(SoldParticipationCache.class);
        this.listedSoldRemotely = Reloadable.with(() -> retrieveSoldParticipationIds(tenant))
            .reloadAfter(Duration.ofMinutes(5))
            .async() // Don't block for this.
            .build();
    }

    private static Set<Long> retrieveSoldParticipationIds(final Tenant tenant) {
        return tenant.call(Zonky::getSoldInvestments)
            .mapToLong(Investment::getId)
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

    public synchronized LongStream getOffered() {
        return state.getValues(KEY)
            .orElse(Stream.empty())
            .mapToLong(Long::parseLong);
    }

    private synchronized void setOffered(final LongStream values) {
        state.update(m -> m.put(KEY, values.mapToObj(String::valueOf)));
    }

    public synchronized void unmarkAsOffered(final long investmentId) {
        setOffered(getOffered().filter(i -> i != investmentId));
    }

    public void markAsOffered(final long investmentId) {
        final LongStream existingValue = getOffered();
        final LongStream newValues = LongStream.concat(existingValue, LongStream.of(investmentId))
            .distinct();
        setOffered(newValues);
    }

    public void markAsSold(final long investmentId) {
        listedSoldLocally.add(investmentId);
        unmarkAsOffered(investmentId);
    }

    public boolean wasOnceSold(final long investmentId) {
        return listedSoldLocally.contains(investmentId) ||
                listedSoldRemotely.get()
                    .mapRight(s -> s.contains(investmentId))
                    .getOrElseGet(ex -> {
                        LOGGER.info("Failed retrieving sold investments from Zonky.", ex);
                        return false;
                    });
    }
}
