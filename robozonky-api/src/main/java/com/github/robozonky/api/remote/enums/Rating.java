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

package com.github.robozonky.api.remote.enums;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.SortedMap;
import java.util.stream.Stream;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.api.Settings;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.internal.util.Maps;

import static com.github.robozonky.internal.util.BigDecimalCalculator.minus;
import static com.github.robozonky.internal.util.BigDecimalCalculator.times;
import static com.github.robozonky.internal.util.Maps.entry;

public enum Rating implements BaseEnum {

    // it is imperative for proper functioning of strategy algorithms that ratings here be ordered best to worst
    AAAAAA("2.99", "0.45", "0.2"),
    AAAAA("3.99", "0.45", "0.2"),
    AAAA("4.99", "0.56", "0.5"),
    AAA("5.99", "0.77", "1.0"),
    AAE("6.99", "1.11", "1.5", null),
    AA("8.49", "1.65", "2.2", "2.5"),
    AE("9.49", "2.21", "2.5", null),
    A("10.99", "2.98", "3.0"),
    B("13.49", "4.11", "3.5"),
    C("15.49", "5.23", "4.0"),
    D("19.99", "8.15", "5.0");

    static final Instant MIDNIGHT_2017_09_01 =
            LocalDate.of(2017, 9, 1).atStartOfDay(Defaults.ZONE_ID).toInstant();
    static final Instant MIDNIGHT_2019_03_18 =
            LocalDate.of(2019, 3, 18).atStartOfDay(Defaults.ZONE_ID).toInstant();
    private static final Ratio ONE_PERCENT = Ratio.fromPercentage(1);
    private static final SortedMap<Integer, Ratio> FEE_DISCOUNTS = Maps.ofEntriesSorted(
            entry(150_000, Ratio.fromPercentage(5)),
            entry(200_000, Ratio.fromPercentage(10)),
            entry(500_000, Ratio.fromPercentage(15)),
            entry(1_000_000, Ratio.fromPercentage(20)));

    private final String code;
    private final Ratio interestRate;
    private final Ratio riskRate;
    private final Ratio currentFee;
    private final Ratio feeBeforeMarch2019;

    Rating(final String code, final String riskRate, final String currentFee, final String feeBeforeMarch2019) {
        this.code = code;
        this.interestRate = Ratio.fromPercentage(code);
        this.riskRate = Ratio.fromPercentage(riskRate);
        this.currentFee = Ratio.fromPercentage(currentFee);
        this.feeBeforeMarch2019 = feeBeforeMarch2019 == null ? Ratio.ZERO : Ratio.fromPercentage(feeBeforeMarch2019);
    }

    Rating(final String code, final String riskRate, final String currentFee) {
        this(code, riskRate, currentFee, currentFee);
    }

    /**
     * @param totalInvested
     * @return
     * @see "https://zonky.cz/zonky-vyhody/"
     */
    private static Ratio feeDiscount(final long totalInvested) {
        /* For the institutional investor ("zonky"), total will cause int overflow. Cap the value be max integer value,
         * since the only place where it will be used in this method doesn't use more than the order of millions.
         */
        final int totalCapped = (totalInvested > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) totalInvested + 1;
        final SortedMap<Integer, Ratio> applicableDiscounts = FEE_DISCOUNTS.headMap(totalCapped);
        if (applicableDiscounts.isEmpty()) {
            return Ratio.ZERO;
        } else {
            return applicableDiscounts.get(applicableDiscounts.lastKey());
        }
    }

    public static Rating findByCode(final String code) {
        return Stream.of(Rating.values())
                .filter(r -> Objects.equals(r.code, code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown rating: " + code));
    }

    private static boolean isBeforeLatestFeeChange(final Instant dateForFees) {
        return dateForFees.isBefore(MIDNIGHT_2019_03_18);
    }

    private static Ratio discount(final Ratio ratio, final long totalInvested) {
        final BigDecimal baseFee = ratio.bigDecimalValue();
        final BigDecimal feeDiscount = feeDiscount(totalInvested).bigDecimalValue();
        return Ratio.fromRaw(minus(baseFee, times(baseFee, feeDiscount)));
    }

    public Ratio getInterestRate() {
        return interestRate;
    }

    public Ratio getFee(final Instant dateForFees) {
        return getFee(dateForFees, 0);
    }

    public Ratio getFee() {
        return getFee(DateUtil.now());
    }

    public Ratio getFee(final long totalInvested) {
        return getFee(DateUtil.now(), totalInvested);
    }

    public Ratio getFee(final Instant dateForFees, final long totalInvested) {
        if (isInvalid(dateForFees)) {
            return Ratio.ZERO;
        } else if (dateForFees.isBefore(MIDNIGHT_2017_09_01)) {
            return discount(ONE_PERCENT, totalInvested);
        } else if (isBeforeLatestFeeChange(dateForFees)) {
            return discount(feeBeforeMarch2019, totalInvested);
        } else {
            return discount(currentFee, totalInvested);
        }
    }

    private boolean isInvalid(final Instant dateForFees) {
        return isBeforeLatestFeeChange(dateForFees) && (this == Rating.AAE || this == Rating.AE);
    }

    public Ratio getMinimalRevenueRate(final Instant dateForFees, final long totalInvested) {
        if (isInvalid(dateForFees)) {
            return Ratio.ZERO;
        }
        final BigDecimal base = getMaximalRevenueRate(dateForFees, totalInvested).bigDecimalValue();
        final BigDecimal risk = riskRate.bigDecimalValue();
        final BigDecimal result = minus(base, risk);
        return Ratio.fromRaw(result);
    }

    public Ratio getMinimalRevenueRate() {
        return getMinimalRevenueRate(DateUtil.now());
    }

    public Ratio getMinimalRevenueRate(final Instant dateForFees) {
        return getMinimalRevenueRate(dateForFees, 0);
    }

    public Ratio getMinimalRevenueRate(final long totalInvested) {
        return getMinimalRevenueRate(DateUtil.now(), totalInvested);
    }

    public Ratio getMaximalRevenueRate(final Instant dateForFees, final long totalInvested) {
        if (isInvalid(dateForFees)) {
            return Ratio.ZERO;
        }
        final BigDecimal base = interestRate.bigDecimalValue();
        final BigDecimal fees = getFee(dateForFees, totalInvested).bigDecimalValue();
        final BigDecimal result = minus(base, fees);
        return Ratio.fromRaw(result);
    }

    public Ratio getMaximalRevenueRate() {
        return getMaximalRevenueRate(DateUtil.now());
    }

    public Ratio getMaximalRevenueRate(final long totalInvested) {
        return getMaximalRevenueRate(DateUtil.now(), totalInvested);
    }

    public Ratio getMaximalRevenueRate(final Instant dateForFees) {
        return getMaximalRevenueRate(dateForFees, 0);
    }

    public Duration getCaptchaDelay() {
        return Settings.INSTANCE.getCaptchaDelay(this);
    }

    @Override
    public String getCode() {
        return code;
    }

}
