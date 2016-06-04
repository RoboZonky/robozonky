/*
 * Copyright 2016 Lukáš Petrovický
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
package com.github.triceo.robozonky.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;

public class InvestmentStrategy {

    protected static final int MINIMAL_INVESTMENT_INCREMENT = 200;
    private final Map<Rating, StrategyPerRating> individualStrategies = new EnumMap<>(Rating.class);

    InvestmentStrategy(final Map<Rating, StrategyPerRating> individualStrategies) {
        for (final Rating r: Rating.values()) {
            if (!individualStrategies.containsKey(r)) {
                throw new IllegalArgumentException("Missing strategy for rating " + r);
            }
            final StrategyPerRating s = individualStrategies.get(r);
            this.individualStrategies.put(r, s);
        }
    }

    public BigDecimal getTargetShare(final Rating r) {
        return this.individualStrategies.get(r).getTargetShare();
    }

    public boolean prefersLongerTerms(final Rating r) {
        return this.individualStrategies.get(r).isPreferLongerTerms();
    }

    public boolean isAcceptable(final Loan loan) {
        return individualStrategies.get(loan.getRating()).isAcceptable(loan);
    }

    public int recommendInvestmentAmount(final Loan loan, final BigDecimal balance) {
        final BigDecimal maxAllowedInvestmentIncrement =
                BigDecimal.valueOf(InvestmentStrategy.MINIMAL_INVESTMENT_INCREMENT);
        BigDecimal tmp = BigDecimal.valueOf(individualStrategies.get(loan.getRating()).recommendInvestmentAmount(loan));
        // round to nearest lower increment
        tmp = tmp.min(balance);
        tmp = tmp.divide(maxAllowedInvestmentIncrement, 0, RoundingMode.DOWN); // make sure we never exceed max allowed
        tmp = tmp.multiply(maxAllowedInvestmentIncrement);
        // make sure we never submit more than there is remaining in the loan
        return Math.min(tmp.intValue(), (int) loan.getRemainingInvestment());
    }

}
