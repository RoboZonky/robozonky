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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoanCache {

    public static final LoanCache INSTANCE = new LoanCache();
    private static final Logger LOGGER = LoggerFactory.getLogger(LoanCache.class);
    private static final Duration EVICT_AFTER = Duration.ofDays(2); // maximum time a loan stays on the marketplace

    private ConcurrentMap<Integer, Loan> cache;
    private ConcurrentMap<Integer, Instant> storedOn;
    private final Lock updateLock = new ReentrantLock(true);

    LoanCache() {
        clean();
    }

    private void evict() {
        final Instant deadline = Instant.now().minus(EVICT_AFTER);
        final Set<Integer> loansToEvict = storedOn.entrySet().stream()
                .filter(e -> e.getValue().isBefore(deadline))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        runLocked(() -> loansToEvict.forEach(loanId -> {
            cache.remove(loanId);
            storedOn.remove(loanId);
        }));
    }

    private void runLocked(final Runnable runnable) {
        updateLock.lock();
        try {
            runnable.run();
        } finally {
            updateLock.unlock();
        }
    }

    public Optional<Loan> getLoan(final int loanId) {
        evict();
        return Optional.ofNullable(cache.get(loanId));
    }

    boolean addLoan(final int loanId, final Loan loan) {
        if (loan.getRemainingInvestment() > 0) {
            return false;
        }
        runLocked(() -> {
            cache.put(loanId, loan);
            storedOn.put(loanId, Instant.now());
        });
        return true;
    }

    public Loan getLoan(final int loanId, final Zonky api) {
        return getLoan(loanId)
                .orElseGet(() -> {
                    LOGGER.trace("Cache miss for loan #{}.", loanId);
                    final Loan l = api.getLoan(loanId);
                    addLoan(loanId, l);
                    return l;
                });
    }

    public void clean() {
        runLocked(() -> {
            cache = new ConcurrentHashMap<>(0);
            storedOn = new ConcurrentHashMap<>(0);
        });
    }
}
