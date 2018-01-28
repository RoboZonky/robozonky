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

package com.github.robozonky.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.stream.IntStream;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.api.Defaults;

import static com.github.robozonky.util.BigDecimalCalculator.divide;
import static com.github.robozonky.util.BigDecimalCalculator.minus;
import static com.github.robozonky.util.BigDecimalCalculator.plus;
import static com.github.robozonky.util.BigDecimalCalculator.times;
import static com.github.robozonky.util.BigDecimalCalculator.toScale;

public class FinancialCalculator {

    private static final Instant MIDNIGHT_2017_09_01 =
            LocalDate.of(2017, 9, 1).atStartOfDay(Defaults.ZONE_ID).toInstant();
    private static final BigDecimal ONE_PERCENT = new BigDecimal("0.01"), FIVE_PERCENT = new BigDecimal("0.05"),
            TEN_PERCENT = new BigDecimal("0.1"), FIFTEEN_PERCENT = new BigDecimal("0.15"),
            TWENTY_PERCENT = new BigDecimal("0.2");
    private static final EnumMap<Rating, BigDecimal> FEES = new EnumMap<Rating, BigDecimal>(Rating.class) {{
        put(Rating.AAAAA, new BigDecimal("0.002"));
        put(Rating.AAAA, new BigDecimal("0.005"));
        put(Rating.AAA, ONE_PERCENT);
        put(Rating.AA, new BigDecimal("0.025"));
        put(Rating.A, new BigDecimal("0.03"));
        put(Rating.B, new BigDecimal("0.035"));
        put(Rating.C, new BigDecimal("0.04"));
        put(Rating.D, FIVE_PERCENT);
    }};

    private static BigDecimal baseFee(final Investment investment) {
        if (investment.getInvestmentDate().toInstant().isBefore(MIDNIGHT_2017_09_01)) {
            return ONE_PERCENT;
        }
        return FEES.get(investment.getRating());
    }

    /**
     * @param portfolioOverview
     * @return
     * @see "https://zonky.cz/zonky-vyhody/"
     */
    private static BigDecimal feeDiscount(final PortfolioOverview portfolioOverview) {
        final int totalInvested = portfolioOverview.getCzkInvested();
        if (totalInvested > 999_999) {
            return TWENTY_PERCENT;
        } else if (totalInvested > 499_999) {
            return FIFTEEN_PERCENT;
        } else if (totalInvested > 199_999) {
            return TEN_PERCENT;
        } else if (totalInvested > 149_999) {
            return FIVE_PERCENT;
        } else {
            return BigDecimal.ZERO;
        }
    }

    public static BigDecimal estimateFeeRate(final Investment investment, final PortfolioOverview portfolioOverview) {
        final BigDecimal baseFee = baseFee(investment);
        final BigDecimal feeDiscount = feeDiscount(portfolioOverview);
        return minus(baseFee, times(baseFee, feeDiscount));
    }

    private static BigDecimal feePaid(final Investment investment, final PortfolioOverview portfolioOverview,
                                      final int totalMonths) {
        if (totalMonths == 0) {
            return BigDecimal.ZERO;
        }
        final BigDecimal originalValue = InvestmentInference.getOriginalAmount(investment);
        final BigDecimal monthlyRate = divide(investment.getInterestRate(), 12);
        final BigDecimal monthlyFee = divide(estimateFeeRate(investment, portfolioOverview), 12);
        final BigDecimal pmt = FinancialUtil.pmt(monthlyRate, investment.getLoanTermInMonth(), originalValue);
        final BigDecimal result = IntStream.range(0, totalMonths)
                .mapToObj(term -> FinancialUtil.fv(monthlyRate, term + 1, pmt, originalValue).negate())
                .map(fv -> times(fv, monthlyFee))
                .reduce(BigDecimal.ZERO, BigDecimalCalculator::plus);
        return toScale(result);
    }

    private static BigDecimal expectedInterest(final Investment i, final int totalMonths) {
        final BigDecimal monthlyRate = divide(i.getInterestRate(), 12);
        return IntStream.range(0, totalMonths)
                .mapToObj(month -> FinancialUtil.ipmt(monthlyRate, month + 1, totalMonths, i.getRemainingPrincipal()))
                .reduce(BigDecimal.ZERO, BigDecimalCalculator::plus).negate();
    }

    private static BigDecimal expectedInterest(final Investment i) {
        return expectedInterest(i, i.getLoanTermInMonth());
    }

    public static BigDecimal actualInterestAfterFees(final Investment investment,
                                                     final PortfolioOverview portfolioOverview,
                                                     final boolean includeSmpSaleFee) {
        final int termsInZonky = investment.getLoanTermInMonth() - investment.getCurrentTerm();
        final BigDecimal fee = feePaid(investment, portfolioOverview, termsInZonky);
        final BigDecimal actualFee = includeSmpSaleFee ? plus(fee, investment.getSmpFee()) : fee;
        final BigDecimal interest = plus(investment.getPaidInterest(), investment.getPaidPenalty());
        return minus(interest, actualFee);
    }

    public static BigDecimal actualInterestAfterFees(final Investment investment,
                                                     final PortfolioOverview portfolioOverview) {
        return actualInterestAfterFees(investment, portfolioOverview, false);
    }

    public static BigDecimal expectedInterestAfterFees(final Investment investment,
                                                       final PortfolioOverview portfolioOverview) {
        final int terms = investment.getRemainingMonths();
        final BigDecimal expectedFee = feePaid(investment, portfolioOverview, terms);
        final BigDecimal expectedIncome = expectedInterest(investment);
        return minus(expectedIncome, expectedFee);
    }

    public static BigDecimal expectedInterestRateAfterFees(final Investment investment,
                                                           final PortfolioOverview portfolioOverview) {
        final BigDecimal interestRate = investment.getInterestRate();
        final BigDecimal feeRate = estimateFeeRate(investment, portfolioOverview);
        return minus(interestRate, feeRate);
    }
}
