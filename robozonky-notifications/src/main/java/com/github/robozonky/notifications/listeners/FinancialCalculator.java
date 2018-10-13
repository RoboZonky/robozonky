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

package com.github.robozonky.notifications.listeners;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.IntStream;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.util.BigDecimalCalculator;
import com.github.robozonky.internal.util.LazyInitialized;
import com.github.robozonky.internal.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;
import static com.github.robozonky.internal.util.BigDecimalCalculator.minus;
import static com.github.robozonky.internal.util.BigDecimalCalculator.plus;
import static com.github.robozonky.internal.util.BigDecimalCalculator.times;
import static com.github.robozonky.internal.util.BigDecimalCalculator.toScale;
import static com.github.robozonky.internal.util.Maps.entry;

final class FinancialCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinancialCalculator.class);
    private static final Instant MIDNIGHT_2017_09_01 =
            LocalDate.of(2017, 9, 1).atStartOfDay(Defaults.ZONE_ID).toInstant();
    private static final BigDecimal ONE_PERCENT = new BigDecimal("0.01"), FIVE_PERCENT = new BigDecimal("0.05"),
            TEN_PERCENT = new BigDecimal("0.1"), FIFTEEN_PERCENT = new BigDecimal("0.15"),
            TWENTY_PERCENT = new BigDecimal("0.2");
    private static final LazyInitialized<Map<Rating, BigDecimal>> FEES = LazyInitialized.create(() -> {
        final Map<Rating, BigDecimal> result = new EnumMap<>(Rating.class);
        result.put(Rating.AAAAA, new BigDecimal("0.002"));
        result.put(Rating.AAAA, new BigDecimal("0.005"));
        result.put(Rating.AAA, ONE_PERCENT);
        result.put(Rating.AA, new BigDecimal("0.025"));
        result.put(Rating.A, new BigDecimal("0.03"));
        result.put(Rating.B, new BigDecimal("0.035"));
        result.put(Rating.C, new BigDecimal("0.04"));
        result.put(Rating.D, FIVE_PERCENT);
        return result;
    });
    private static final SortedMap<Integer, BigDecimal> FEE_DISCOUNTS = Maps.ofEntriesSorted(
            entry(150_000, FIVE_PERCENT),
            entry(200_000, TEN_PERCENT),
            entry(500_000, FIFTEEN_PERCENT),
            entry(1_000_000, TWENTY_PERCENT));

    private FinancialCalculator() {
        // no instances
    }

    private static BigDecimal baseFee(final Investment investment) {
        final OffsetDateTime investmentDate = investment.getInvestmentDate();
        if (investmentDate.toInstant().isBefore(MIDNIGHT_2017_09_01)) {
            return ONE_PERCENT;
        }
        return FEES.get().get(investment.getRating());
    }

    /**
     * @param totalInvested
     * @return
     * @see "https://zonky.cz/zonky-vyhody/"
     */
    private static BigDecimal feeDiscount(final long totalInvested) {
        /* For the institutional investor ("zonky"), total will cause int overflow. Cap the value be max integer value,
         * since the only place where it will be used in this method doesn't use more than the order of millions.
         */
        final int totalCapped = (totalInvested > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)totalInvested + 1;
        final SortedMap<Integer, BigDecimal> applicableDiscounts = FEE_DISCOUNTS.headMap(totalCapped);
        if (applicableDiscounts.isEmpty()) {
            return BigDecimal.ZERO;
        } else {
            return applicableDiscounts.get(applicableDiscounts.lastKey());
        }
    }

    public static BigDecimal estimateFeeRate(final Investment investment,
                                             final long totalInvested) {
        final BigDecimal baseFee = baseFee(investment);
        final BigDecimal feeDiscount = feeDiscount(totalInvested);
        return minus(baseFee, times(baseFee, feeDiscount));
    }

    private static BigDecimal feePaid(final Investment investment, final long totalInvested, final int totalMonths) {
        if (totalMonths == 0) {
            return BigDecimal.ZERO;
        }
        final BigDecimal annualFee = estimateFeeRate(investment, totalInvested);
        return feePaid(investment.getOriginalPrincipal(), investment.getInterestRate(), investment.getOriginalTerm(),
                       annualFee, totalMonths);
    }

    static BigDecimal feePaid(final BigDecimal originalValue, final BigDecimal interestRate,
                              final int originalTerm, final BigDecimal annualFee, final int totalMonths) {
        final BigDecimal monthlyRate = divide(interestRate, 12);
        final BigDecimal monthlyFee = divide(annualFee, 12);
        final BigDecimal pmt = FinancialUtil.pmt(monthlyRate, originalTerm, originalValue);
        final BigDecimal result = IntStream.range(0, totalMonths)
                .mapToObj(term -> FinancialUtil.fv(monthlyRate, term + 1, pmt, originalValue).negate())
                .map(fv -> times(fv, monthlyFee))
                .reduce(BigDecimal.ZERO, BigDecimalCalculator::plus);
        LOGGER.debug("Fee paid from {} CZK at {} rate for {} months with {} annual fee over {} months is {} CZK.",
                     originalValue, interestRate, originalTerm, annualFee, totalMonths, result.doubleValue());
        return toScale(result);
    }

    private static BigDecimal expectedInterest(final Investment i, final int totalMonths) {
        final BigDecimal monthlyRate = divide(i.getInterestRate(), 12);
        return IntStream.range(0, totalMonths)
                .mapToObj(month -> FinancialUtil.ipmt(monthlyRate, month + 1, totalMonths, i.getRemainingPrincipal()))
                .reduce(BigDecimal.ZERO, BigDecimalCalculator::plus).negate();
    }

    private static BigDecimal expectedInterest(final Investment i) {
        return expectedInterest(i, i.getOriginalTerm());
    }

    private static Integer countTermsInZonky(final Investment investment) {
        return investment.getPaymentStatus()
                .filter(s -> s == PaymentStatus.PAID)
                .map(s -> investment.getCurrentTerm())
                .orElse(investment.getCurrentTerm() - investment.getRemainingMonths());
    }

    public static BigDecimal actualInterestAfterFees(final Investment investment, final long totalInvested,
                                                     final boolean includeSmpSaleFee) {
        final int termsInZonky = countTermsInZonky(investment);
        final BigDecimal fee = feePaid(investment, totalInvested, termsInZonky);
        final BigDecimal actualFee = includeSmpSaleFee ? plus(fee,
                                                              investment.getSmpFee().orElse(BigDecimal.ZERO)) : fee;
        final BigDecimal interest = plus(investment.getPaidInterest(), investment.getPaidPenalty());
        return minus(interest, actualFee);
    }

    public static BigDecimal actualInterestAfterFees(final Investment investment, final long totalInvested) {
        return actualInterestAfterFees(investment, totalInvested, false);
    }

    public static BigDecimal expectedInterestAfterFees(final Investment investment, final long totalInvested) {
        final int terms = investment.getRemainingMonths();
        final BigDecimal expectedFee = feePaid(investment, totalInvested, terms);
        final BigDecimal expectedIncome = expectedInterest(investment);
        return minus(expectedIncome, expectedFee);
    }

    public static BigDecimal expectedInterestRateAfterFees(final Investment investment, final long totalInvested) {
        final BigDecimal interestRate = investment.getInterestRate();
        final BigDecimal feeRate = estimateFeeRate(investment, totalInvested);
        return minus(interestRate, feeRate);
    }
}
