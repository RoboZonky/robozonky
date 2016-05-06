/*
 * Copyright 2016 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.petrovicky.zonkybot.strategy;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import net.petrovicky.zonkybot.remote.Loan;
import net.petrovicky.zonkybot.remote.Rating;

public class InvestmentStrategy {

    public boolean isAcceptable(Loan loan) {
        Rating r = loan.getRating();
        return individualStrategies.get(r).isAcceptable(loan);
    }

    private final Map<Rating, StrategyPerRating> individualStrategies = new EnumMap<>(Rating.class);
    private final Map<Rating, BigDecimal> targetShares = new EnumMap<>(Rating.class);
    private final Map<Rating, Integer> minimumInvestmentAmounts = new EnumMap<>(Rating.class);
    private final Map<Rating, Integer> maximumInvestmentAmounts = new EnumMap<>(Rating.class);
    private final Map<Rating, Boolean> prefersLongerTerms = new EnumMap<>(Rating.class);
    private int minimumInvestmentAmount = Integer.MAX_VALUE;

    InvestmentStrategy(Map<Rating, StrategyPerRating> individualStrategies) {
        this.individualStrategies.putAll(individualStrategies);
        for (StrategyPerRating s : individualStrategies.values()) {
            minimumInvestmentAmount = Math.min(s.getMinimumInvestmentAmount(), minimumInvestmentAmount);
            targetShares.put(s.getRating(), s.getTargetShare());
            minimumInvestmentAmounts.put(s.getRating(), s.getMinimumInvestmentAmount());
            maximumInvestmentAmounts.put(s.getRating(), s.getMaximumInvestmentAmount());
            prefersLongerTerms.put(s.getRating(), s.isPreferLongerTerms());
        }
    }

    public BigDecimal getTargetShare(final Rating r) {
        return targetShares.get(r);
    }

    public int getMinimumInvestmentAmount() {
        return minimumInvestmentAmount;
    }

    public boolean prefersLongerTerms(final Rating r) {
        return prefersLongerTerms.get(r);
    }

    public int getMinimumInvestmentAmount(final Rating r) {
        return minimumInvestmentAmounts.get(r);
    }

    public int getMaximumInvestmentAmount(final Rating r) {
        return maximumInvestmentAmounts.get(r);
    }

}
