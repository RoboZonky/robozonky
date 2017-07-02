/*
 * Copyright 2017 Lukáš Petrovický
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

import java.math.BigInteger;
import java.util.EnumMap;
import java.util.Map;

import com.github.triceo.robozonky.api.remote.enums.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvestmentSize {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvestmentSize.class);

    private DefaultInvestmentSize defaultInvestmentSize = new DefaultInvestmentSize(BigInteger.valueOf(200));
    private final Map<Rating, BigInteger> minimumInvestments = new EnumMap<>(Rating.class),
            maximumInvestments = new EnumMap<>(Rating.class);

    public void setDefaultInvestmentSize(final DefaultInvestmentSize size) {
        this.defaultInvestmentSize = size;
        InvestmentSize.LOGGER.debug("Default investment size to be between {},- and {},- CZK.",
                size.getMinimumInvestmentInCzk(), size.getMaximumInvestmentInCzk());
    }

    public void addItem(final InvestmentSizeItem item) {
        minimumInvestments.put(item.getRating(), item.getMininumInvestmentInCzk());
        maximumInvestments.put(item.getRating(), item.getMaximumInvestmentInCzk());
        InvestmentSize.LOGGER.debug("Target investment size for rating {} to be between {},- and {},- CZK.",
                item.getRating(), item.getMininumInvestmentInCzk(), item.getMaximumInvestmentInCzk());
    }

    public BigInteger getMinimumInvestmentSizeInCzk(final Rating rating) {
        if (minimumInvestments.containsKey(rating)) {
            return minimumInvestments.get(rating);
        } else { // no minimum share specified; use default
            return defaultInvestmentSize.getMinimumInvestmentInCzk();
        }
    }

    public BigInteger getMaximumInvestmentSizeInCzk(final Rating rating) {
        if (maximumInvestments.containsKey(rating)) {
            return maximumInvestments.get(rating);
        } else { // no maximum share specified; use default
            return defaultInvestmentSize.getMaximumInvestmentInCzk();
        }
    }
}
