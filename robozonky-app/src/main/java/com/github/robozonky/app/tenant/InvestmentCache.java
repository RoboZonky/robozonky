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
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.common.async.Tasks;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.test.DateUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class InvestmentCache implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Duration EVICT_AFTER = Duration.ofHours(1);
    private static final Duration EVICT_EVERY = Duration.ofMinutes(30);

    private final Tenant tenant;
    private final Map<Integer, Tuple2<Investment, Instant>> storage = new ConcurrentHashMap<>(20);
    private final ScheduledFuture<?> evictionTask;

    public InvestmentCache(final Tenant tenant) {
        LOGGER.debug("Starting for {}.", tenant);
        this.tenant = tenant;
        this.evictionTask = Tasks.BACKGROUND.scheduler().submit(this::evict, EVICT_EVERY, EVICT_EVERY);
    }

    private static boolean isExpired(final Tuple2<Investment, Instant> p) {
        final Instant now = DateUtil.now();
        final Instant expiration = p._2().plus(EVICT_AFTER);
        return expiration.isBefore(now);
    }

    private void evict() {
        LOGGER.trace("Evicting investments.");
        storage.entrySet().stream()
                .filter(e -> isExpired(e.getValue()))
                .forEach(e -> storage.remove(e.getKey()));
        LOGGER.trace("Evicted.");
    }

    Optional<Investment> getFromCache(final int loanId) {
        final Tuple2<Investment, Instant> result = storage.get(loanId);
        if (result == null || isExpired(result)) {
            LOGGER.trace("Miss for loan #{}.", loanId);
            return Optional.empty();
        } else {
            LOGGER.trace("Hit for loan #{}.", loanId);
            return Optional.of(result._1());
        }
    }

    private void add(final int loanId, final Investment item) {
        storage.put(loanId, Tuple.of(item, DateUtil.now()));
    }

    public Investment getInvestment(final int loanId) {
        return getFromCache(loanId).orElseGet(() -> {
            final Investment item = tenant.call(api -> api.getInvestmentByLoanId(loanId)).orElseThrow();
            add(loanId, item);
            return item;
        });
    }

    /**
     * Will cancel the background operation that evicts stale items from the storage. After this method has been called,
     * the instance in question must not be used anymore and should be left to be picked up by the GC.
     */
    @Override
    public void close() {
        evictionTask.cancel(true);
    }
}
