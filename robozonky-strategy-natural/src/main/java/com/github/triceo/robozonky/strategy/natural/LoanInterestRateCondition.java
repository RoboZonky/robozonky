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

package com.github.triceo.robozonky.strategy.natural;

import java.math.BigDecimal;

public class LoanInterestRateCondition extends AbstractRangeCondition {

    private static final BigDecimal MIN_INCREMENT = BigDecimal.valueOf(Double.MIN_VALUE),
            MAX_RATE = BigDecimal.valueOf(Double.MAX_VALUE);

    public static BigDecimal lessThan(final BigDecimal num) {
        return num.subtract(LoanInterestRateCondition.MIN_INCREMENT);
    }

    public static BigDecimal moreThan(final BigDecimal num) {
        return num.add(LoanInterestRateCondition.MIN_INCREMENT);
    }

    private static void assertIsInRange(final BigDecimal interestRate) {
        final BigDecimal min = BigDecimal.ZERO;
        final BigDecimal max = LoanInterestRateCondition.MAX_RATE;
        if (min.compareTo(interestRate) > 0 || max.compareTo(interestRate) < 0) {
            throw new IllegalArgumentException("Loan interest rate must be in range of <" + min + "; " + max + ">.");
        }
    }

    public LoanInterestRateCondition(final BigDecimal fromInclusive, final BigDecimal toInclusive) {
        super(Wrapper::getInterestRate, fromInclusive, toInclusive);
        LoanInterestRateCondition.assertIsInRange(fromInclusive);
        LoanInterestRateCondition.assertIsInRange(toInclusive);
    }

    public LoanInterestRateCondition(final BigDecimal fromInclusive) {
        this(fromInclusive, LoanInterestRateCondition.MAX_RATE);
    }
}
