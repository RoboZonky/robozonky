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

package com.github.robozonky.strategy.natural.conditions;

import java.math.BigDecimal;

import com.github.robozonky.api.Ratio;

public class LoanInterestRateCondition extends AbstractRangeCondition {

    private static final Ratio MAX_RATE = Ratio.fromRaw(Double.MAX_VALUE);

    public LoanInterestRateCondition(final Ratio fromInclusive, final Ratio toInclusive) {
        super(w -> w.getInterestRate().bigDecimalValue(), fromInclusive.bigDecimalValue(),
              toInclusive.bigDecimalValue());
        LoanInterestRateCondition.assertIsInRange(fromInclusive);
        LoanInterestRateCondition.assertIsInRange(toInclusive);
    }

    public LoanInterestRateCondition(final Ratio fromInclusive) {
        this(fromInclusive, LoanInterestRateCondition.MAX_RATE);
    }

    private static void assertIsInRange(final Ratio interestRate) {
        final BigDecimal min = BigDecimal.ZERO;
        final BigDecimal max = LoanInterestRateCondition.MAX_RATE.bigDecimalValue();
        final BigDecimal target = interestRate.bigDecimalValue();
        if (min.compareTo(target) > 0 || max.compareTo(target) < 0) {
            throw new IllegalArgumentException("Loan interest rate must be in range of <" + min + "; " + max + ">.");
        }
    }
}
