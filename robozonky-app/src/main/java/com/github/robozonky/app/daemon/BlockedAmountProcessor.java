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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.NotFoundException;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.util.LazyInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockedAmountProcessor implements PortfolioDependant {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockedAmountProcessor.class);

    private final Map<Integer, Blocked> syntheticByLoanId = new ConcurrentHashMap<>(0);
    private final AtomicReference<Map<Rating, BigDecimal>> adjustments = new AtomicReference<>(Collections.emptyMap());
    private final AtomicReference<Map<Integer, Blocked>> realById = new AtomicReference<>(Collections.emptyMap());

    public static BlockedAmountProcessor create(final Tenant tenant) {
        final BlockedAmountProcessor result = new BlockedAmountProcessor();
        result.realById.set(result.readBlockedAmounts(tenant));
        result.reset();
        return result;
    }

    public static Supplier<BlockedAmountProcessor> createLazy(final Tenant tenant) {
        return LazyInitialized.create(() -> BlockedAmountProcessor.create(tenant));
    }

    private Map<Integer, Blocked> readBlockedAmounts(final Tenant tenant) {
        final long portfolioSize = tenant.call(Zonky::getStatistics).getCurrentOverview().getPrincipalLeft();
        final Divisor divisor = new Divisor(portfolioSize);
        return tenant.call(Zonky::getBlockedAmounts)
                .parallel()
                .peek(ba -> LOGGER.debug("Found: {}.", ba))
                .filter(ba -> ba.getLoanId() > 0)
                .flatMap(ba -> {
                    final int loanId = ba.getLoanId();
                    try {
                        final Loan l = LoanCache.get().getLoan(loanId, tenant);
                        syntheticByLoanId.remove(loanId); // remove synthetic
                        return Stream.of(new Blocked(ba, l.getRating()));
                    } catch (final NotFoundException ex) {
                        /*
                         * Zonky has an intermittent caching problem and a failure here would prevent the robot from
                         * ever finishing the portfolio update. As a result, the robot would not be able to do
                         * anything. Comparatively, being wrong by 0,5 % is not so bad.
                         */
                        LOGGER.warn("Zonky API mistakenly reports loan #{} as non-existent. " +
                                            "Consider reporting this to Zonky so that they can fix it.",
                                    loanId, ex);
                        if (syntheticByLoanId.containsKey(loanId)) { // we have the loan as synthetic, no need to worry
                            return Stream.empty();
                        }
                        final BigDecimal amount = ba.getAmount();
                        divisor.add(amount.longValue());
                        final long shareThatIsWrongPerMille = divisor.getSharePerMille();
                        if (shareThatIsWrongPerMille >= 5) {
                            throw new IllegalStateException("RoboZonky portfolio structure is too far off.", ex);
                        } else { // let this slide as the portfolio is only a little bit off
                            return Stream.empty();
                        }
                    }
                })
                .collect(Collectors.toMap(Blocked::getId, b -> b));
    }

    private void reset() {
        adjustments.set(null);
    }

    void simulateCharge(final int loanId, final Rating rating, final BigDecimal amount) {
        syntheticByLoanId.put(loanId, new Blocked(amount, rating));
        reset();
    }

    private Map<Rating, BigDecimal> calculateAdjustments() {
        return Stream.concat(realById.get().values().stream(), syntheticByLoanId.values().stream())
                .collect(Collectors.groupingBy(Blocked::getRating,
                                               () -> new EnumMap<>(Rating.class),
                                               Collectors.reducing(BigDecimal.ZERO, Blocked::getAmount,
                                                                   BigDecimal::add)));
    }

    private BigDecimal calculateUnprocessedBlockedBalance() {
        return Stream.concat(realById.get().values().stream(), syntheticByLoanId.values().stream())
                .map(Blocked::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    Map<Rating, BigDecimal> getAdjustments() {
        return adjustments.updateAndGet(old -> {
            if (old != null) {
                return old;
            }
            final Map<Rating, BigDecimal> result = calculateAdjustments();
            LOGGER.debug("New adjustments: {}.", result);
            return result;
        });
    }

    @Override
    public void accept(final TransactionalPortfolio transactional) {
        final BigDecimal before = calculateUnprocessedBlockedBalance();
        realById.set(readBlockedAmounts(transactional.getTenant()));
        final BigDecimal after = calculateUnprocessedBlockedBalance();
        transactional.getPortfolio().getRemoteBalance().update(after.subtract(before));
        reset();
    }

    private static final class Blocked {

        private final int id;
        private final BigDecimal amount;
        private final Rating rating;

        public Blocked(final BigDecimal amount, final Rating rating) {
            this.id = -1;
            this.amount = amount.abs();
            this.rating = rating;
        }

        public Blocked(final BlockedAmount amount, final Rating rating) {
            this.id = amount.getId();
            this.amount = amount.getAmount().abs();
            this.rating = rating;
        }

        public int getId() {
            return id;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public Rating getRating() {
            return rating;
        }
    }
}
