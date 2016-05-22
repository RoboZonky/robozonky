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
package com.github.triceo.robozonky.strategy;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import com.github.triceo.robozonky.remote.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyBuilder.class);

    private final Map<Rating, StrategyPerRating> individualStrategies = new EnumMap<>(Rating.class);

    public StrategyBuilder addIndividualStrategy(final Rating r, final BigDecimal targetShare, final int minTerm,
                                                 final int maxTerm, final int maxLoanAmount,
                                                 final BigDecimal maxLoanShare, final boolean preferLongerTerms) {
        if (individualStrategies.containsKey(r)) {
            throw new IllegalArgumentException("Already added strategy for rating " + r);
        }
        individualStrategies.put(r,
                new StrategyPerRating(r, targetShare, minTerm, maxTerm, maxLoanAmount, maxLoanShare, preferLongerTerms));
        StrategyBuilder.LOGGER.debug("Adding strategy for rating '{}'.", r.getCode());
        StrategyBuilder.LOGGER.debug("Target share for rating '{}' among total investments is {}.", r.getCode(), targetShare);
        StrategyBuilder.LOGGER.debug("Range of acceptable investment terms for rating '{}' is <{}, {}> months.", r.getCode(),
                minTerm == -1 ? 0 : minTerm, maxTerm == -1 ? "+inf" : maxTerm);
        StrategyBuilder.LOGGER.debug("Maximum investment amount for rating '{}' is {} CZK.", r.getCode(), maxLoanAmount);
        StrategyBuilder.LOGGER.debug("Maximum investment share for rating '{}' is {}.", r.getCode(), maxLoanShare);
        StrategyBuilder.LOGGER.debug("Rating '{}' will prefer longer terms.", r.getCode());
        return this;
    }

    public InvestmentStrategy build() {
        if (individualStrategies.size() != Rating.values().length) {
            throw new IllegalStateException("Strategy is incomplete.");
        }
        return new InvestmentStrategy(individualStrategies);
    }

}
