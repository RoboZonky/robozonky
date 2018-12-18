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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.util.DateUtil;
import com.github.robozonky.util.Scheduler;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoanCache {

    private static final int INITIAL_CACHE_SIZE = 20;
    private static final Duration EVICT_AFTER = Duration.ofHours(1);
    private static final Lazy<LoanCache> INSTANCE = Lazy.of(LoanCache::new);
    private final Logger LOGGER = LoggerFactory.getLogger(LoanCache.class);
    private final Lock updateLock = new ReentrantLock(true);
    private final AtomicReference<Map<Integer, Tuple2<Loan, Instant>>> cache = new AtomicReference<>();

    LoanCache() {
        clean();
        final Duration thirtyMinutes = Duration.ofMinutes(30);
        Scheduler.inBackground().submit(this::evict, thirtyMinutes, thirtyMinutes); // schedule automated eviction
    }

    public static LoanCache get() {
        return INSTANCE.get();
    }

    private static boolean isExpired(final Tuple2<Loan, Instant> p) {
        final Instant now = DateUtil.now();
        final Instant expiration = p._2().plus(EVICT_AFTER);
        return expiration.isBefore(now);
    }

    private void evict() {
        LOGGER.trace("Evicting loans.");
        runLocked(() -> cache.updateAndGet(storage -> storage.entrySet().stream()
                .filter(e -> !isExpired(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a,
                                          () -> new ConcurrentHashMap<>(INITIAL_CACHE_SIZE)))));
        LOGGER.trace("Evicted.");
    }

    private void runLocked(final Runnable runnable) {
        updateLock.lock();
        try {
            runnable.run();
        } finally {
            updateLock.unlock();
        }
    }

    private <T> T callLocked(final Supplier<T> runnable) {
        updateLock.lock();
        try {
            return runnable.get();
        } finally {
            updateLock.unlock();
        }
    }

    Optional<Loan> getLoan(final int loanId) {
        final Tuple2<Loan, Instant> result = callLocked(() -> cache.get().get(loanId));
        if (result == null || isExpired(result)) {
            LOGGER.trace("Cache miss for loan #{}.", loanId);
            return Optional.empty();
        } else {
            return Optional.of(result._1());
        }
    }

    private void addLoan(final int loanId, final Loan loan) {
        runLocked(() -> cache.get().put(loanId, Tuple.of(loan, DateUtil.now())));
    }

    public Loan getLoan(final int loanId, final Tenant tenant) {
        return getLoan(loanId).orElseGet(() -> {
            final Loan l = tenant.call(api -> api.getLoan(loanId));
            if (l.getRemainingInvestment() > 0) {  // prevent caching information which will soon be outdated
                LOGGER.debug("Not adding loan {} to cache as it is not yet fully invested.", l);
            } else {
                addLoan(loanId, l);
            }
            return l;
        });
    }

    public Loan getLoan(final Investment investment, final Tenant tenant) {
        return getLoan(investment.getLoanId(), tenant);
    }

    public void clean() {
        runLocked(() -> cache.set(new ConcurrentHashMap<>(INITIAL_CACHE_SIZE)));
    }

}
