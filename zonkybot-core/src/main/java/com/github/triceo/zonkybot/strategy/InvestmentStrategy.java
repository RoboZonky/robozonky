/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.zonkybot.strategy;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import com.github.triceo.zonkybot.remote.Loan;
import com.github.triceo.zonkybot.remote.Rating;

public class InvestmentStrategy {

    public boolean isAcceptable(final Loan loan) {
        return individualStrategies.get(loan.getRating()).isAcceptable(loan);
    }

    public int recommendInvestmentAmount(final Loan loan) {
        return individualStrategies.get(loan.getRating()).recommendInvestmentAmount(loan);
    }

    private final Map<Rating, StrategyPerRating> individualStrategies = new EnumMap<>(Rating.class);
    private final Map<Rating, BigDecimal> targetShares = new EnumMap<>(Rating.class);
    private final Map<Rating, Boolean> prefersLongerTerms = new EnumMap<>(Rating.class);

    InvestmentStrategy(final Map<Rating, StrategyPerRating> individualStrategies) {
        this.individualStrategies.putAll(individualStrategies);
        for (final StrategyPerRating s : individualStrategies.values()) {
            targetShares.put(s.getRating(), s.getTargetShare());
            prefersLongerTerms.put(s.getRating(), s.isPreferLongerTerms());
        }
    }

    public BigDecimal getTargetShare(final Rating r) {
        return targetShares.get(r);
    }

    public boolean prefersLongerTerms(final Rating r) {
        return prefersLongerTerms.get(r);
    }

}
