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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.internal.async.Tasks;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.internal.test.DateUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class Cache<T> implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(Cache.class);
    private static final Duration EVICT_AFTER = Duration.ofDays(1);
    private static final Duration EVICT_EVERY = Duration.ofHours(1);

    private static final Backend<Loan> LOAN_BACKEND = new Backend<>() {
        @Override
        public Class<Loan> getItemClass() {
            return Loan.class;
        }

        @Override
        public Either<Exception, Loan> getItem(final int id, final Tenant tenant) {
            try {
                return Either.right(tenant.call(zonky -> zonky.getLoan(id)));
            } catch (final Exception ex) {
                return Either.left(ex);
            }
        }

        @Override
        public boolean shouldCache(final Loan item) {
            return item.getRemainingInvestment() == 0;
        }
    };

    private static final Backend<Investment> INVESTMENT_BACKEND = new Backend<>() {
        @Override
        public Class<Investment> getItemClass() {
            return Investment.class;
        }

        @Override
        public Either<Exception, Investment> getItem(final int id, final Tenant tenant) {
            return tenant.call(zonky -> zonky.getInvestmentByLoanId(id))
                    .map(Either::<Exception, Investment>right)
                    .orElseGet(() -> Either.left(new NoSuchElementException(getItemClass() + " #" + id)));
        }

        @Override
        public boolean shouldCache(final Investment item) {
            return true;
        }
    };

    private final Tenant tenant;
    private final Backend<T> backend;
    private final Map<Integer, Tuple2<T, Instant>> storage = new ConcurrentHashMap<>(20);
    private final ScheduledFuture<?> evictionTask;

    private Cache(final Tenant tenant, final Backend<T> backend) {
        LOGGER.debug("Starting {} cache for {}.", backend.getItemClass(), tenant);
        this.tenant = tenant;
        this.backend = backend;
        this.evictionTask = Tasks.BACKGROUND.scheduler().submit(this::evict, EVICT_EVERY, EVICT_EVERY);
    }

    public static Cache<Loan> forLoan(final Tenant tenant) {
        return new Cache<>(tenant, LOAN_BACKEND);
    }

    public static Cache<Investment> forInvestment(final Tenant tenant) {
        return new Cache<>(tenant, INVESTMENT_BACKEND);
    }

    private boolean isExpired(final Tuple2<T, Instant> p) {
        final Instant now = DateUtil.now();
        final Instant expiration = p._2().plus(EVICT_AFTER);
        return expiration.isBefore(now);
    }

    private void evict() {
        LOGGER.trace("Evicting {}.", backend.getItemClass());
        storage.entrySet().stream()
                .filter(e -> isExpired(e.getValue()))
                .forEach(e -> storage.remove(e.getKey()));
        LOGGER.trace("Evicted.");
    }

    Optional<T> getFromCache(final int id) {
        final Tuple2<T, Instant> result = storage.get(id);
        if (result == null || isExpired(result)) {
            LOGGER.trace("Miss for {}.", identify(id));
            return Optional.empty();
        } else {
            LOGGER.trace("Hit for {}.", identify(id));
            return Optional.of(result._1());
        }
    }

    private String identify(final int id) {
        return backend.getItemClass() + " #" + id;
    }

    private void add(final int id, final T item) {
        storage.put(id, Tuple.of(item, DateUtil.now()));
    }

    public T get(final int id) {
        return getFromCache(id).orElseGet(() -> {
            final T item = backend.getItem(id, tenant)
                    .getOrElseThrow(e -> new IllegalStateException("Can not read " + identify(id) + " from Zonky.", e));
            if (backend.shouldCache(item)) {
                add(id, item);
            } else {
                // prevent caching information which will soon be outdated
                LOGGER.debug("Not adding {} as it is not yet fully invested.", identify(id));
            }
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

    private interface Backend<I> {

        Class<I> getItemClass();

        Either<Exception, I> getItem(int id, Tenant tenant);

        boolean shouldCache(I item);
    }
}
