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

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.common.Tenant;

public final class SoldParticipationCache {

    private static final Map<SessionInfo, SoldParticipationCache> INSTANCES = new WeakHashMap<>(0);

    private final Set<Integer> listedSoldLocally = new CopyOnWriteArraySet<>();
    private final ExpiringRemoteSoldSet listedSoldRemotely;

    private SoldParticipationCache(final Tenant tenant) {
        this.listedSoldRemotely = new ExpiringRemoteSoldSet(tenant);
    }

    private static SoldParticipationCache newCache(final Tenant tenant) {
        return new SoldParticipationCache(tenant);
    }

    public static SoldParticipationCache forTenant(final Tenant tenant) {
        return INSTANCES.computeIfAbsent(tenant.getSessionInfo(), key -> newCache(tenant));
    }

    void markAsSold(final int loanId) {
        listedSoldLocally.add(loanId);
    }

    public boolean wasOnceSold(final int loanId) {
        return listedSoldLocally.contains(loanId)
                || listedSoldRemotely.get().map(s -> s.contains(loanId)).orElse(false);
    }
}
