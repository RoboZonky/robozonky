/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.util.Scheduler;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoanCache {

    public static final LoanCache INSTANCE = new LoanCache();
    private static final int INITIAL_CACHE_SIZE = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger(LoanCache.class);
    private static final Duration EVICT_AFTER = Duration.ofHours(1);
    private final Lock updateLock = new ReentrantLock(true);
    private volatile MutableIntObjectMap<Pair<Loan, Instant>> cache;

    LoanCache() {
        clean();
        Scheduler.inBackground().submit(this::evict, Duration.ofMinutes(30)); // schedule automated eviction
    }

    private static boolean isExpired(final Pair<Loan, Instant> p) {
        final Instant deadline = Instant.now().minus(EVICT_AFTER);
        return p.getTwo().isBefore(deadline);
    }

    private void evict() {
        LOGGER.trace("Evicting loans.");
        runLocked(() -> cache = cache.reject((value, p) -> isExpired(p)));
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

    public Optional<Loan> getLoan(final int loanId) {
        return Optional.ofNullable(callLocked(() -> {
            final Pair<Loan, Instant> result = cache.get(loanId);
            if (result == null) {
                LOGGER.trace("Cache miss for loan #{}.", loanId);
                return null;
            } else if (isExpired(result)) {
                LOGGER.trace("Evicting expired loan #{}.", loanId);
                cache.remove(loanId);
                return null;
            } else {
                return result.getOne();
            }
        }));
    }

    private void addLoan(final int loanId, final Loan loan) {
        runLocked(() -> cache.put(loanId, Tuples.pair(loan, Instant.now())));
    }

    public Loan getLoan(final int loanId, final Zonky api) {
        return getLoan(loanId).orElseGet(() -> {
            final Loan l = api.getLoan(loanId);
            addLoan(loanId, l);
            return l;
        });
    }

    public Loan getLoan(final Investment investment, final Zonky api) {
        final Loan loan = getLoan(investment.getLoanId(), api);
        /*
         * investment may have been created without access to the loan. now that we have the loan, we can update the
         * investment, filling any missing information.
         */
        Investment.fillFrom(investment, loan);
        return loan;
    }

    public void clean() {
        runLocked(() -> cache = new IntObjectHashMap<>(INITIAL_CACHE_SIZE));
    }
}
