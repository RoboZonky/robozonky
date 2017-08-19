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

package com.github.triceo.robozonky.api.remote.enums;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.triceo.robozonky.internal.api.Defaults;
import com.github.triceo.robozonky.internal.api.Settings;

public enum Rating implements BaseEnum {

    // it is imperative for proper functioning of strategy algorithms that ratings here be ordered best to worst
    AAAAA("A**", new BigDecimal("0.0299"), new BigDecimal("0.0379"), Duration.ZERO),
    AAAA("A*", new BigDecimal("0.0399"), new BigDecimal("0.0449"), Duration.ZERO),
    AAA("A++", new BigDecimal("0.0499"), Duration.ZERO),
    AA("A+", new BigDecimal("0.0749"), new BigDecimal("0.0599"), Duration.ZERO),
    A("A", new BigDecimal("0.0999"), new BigDecimal("0.0799")),
    B("B", new BigDecimal("0.1249"), new BigDecimal("0.0999")),
    C("C", new BigDecimal("0.1449"), new BigDecimal("0.1149")),
    D("D", new BigDecimal("0.1899"), new BigDecimal("0.1499"));

    private static final BigDecimal SALE_FEE = new BigDecimal("0.015");
    private static final OffsetDateTime MIDNIGHT_2017_09_01 =
            getThreshold(LocalDate.of(2017, Month.SEPTEMBER, 01), Defaults.ZONE_ID);
    private final String code;
    private final BigDecimal oldInterestRate, newInterestRate;
    private final TemporalAmount captchaDelay;

    Rating(final String code, final BigDecimal expectedYieldPre20170901, final BigDecimal expectedYield) {
        this(code, expectedYieldPre20170901, expectedYield, Settings.INSTANCE.getCaptchaDelay());
    }

    Rating(final String code, final BigDecimal expectedYieldPre20170901, final BigDecimal expectedYield,
           final TemporalAmount captchaDelay) {
        this.code = code;
        this.oldInterestRate = expectedYieldPre20170901;
        this.newInterestRate = expectedYield;
        this.captchaDelay = captchaDelay;
    }

    Rating(final String code, final BigDecimal expectedYield, final TemporalAmount captchaDelay) {
        this(code, expectedYield, expectedYield, captchaDelay);
    }

    public static Rating findByCode(final String code) {
        return Stream.of(Rating.values())
                .filter(r -> Objects.equals(r.code, code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown rating: " + code));
    }

    private static OffsetDateTime getThreshold(final LocalDate date, final ZoneId zoneId) {
        final LocalDateTime sep1stMidnight = LocalDateTime.of(date, LocalTime.MIDNIGHT);
        return sep1stMidnight.atZone(zoneId).toOffsetDateTime();
    }

    public TemporalAmount getCaptchaDelay() {
        return captchaDelay;
    }

    @Override
    public String getCode() {
        return code;
    }

    public BigDecimal getInterestRateWithoutFees(final OffsetDateTime investmentDate) {
        if (investmentDate.isAfter(MIDNIGHT_2017_09_01)) {
            return newInterestRate;
        } else {
            return oldInterestRate;
        }
    }

    public BigDecimal getRelativeSaleFee() {
        return SALE_FEE;
    }
}
