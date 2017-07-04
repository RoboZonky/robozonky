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

import java.util.EnumMap;
import java.util.Map;

import com.github.triceo.robozonky.api.remote.enums.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioStructure {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioStructure.class);

    private final Map<Rating, Integer> minimumShares = new EnumMap<>(Rating.class),
            maximumShares = new EnumMap<>(Rating.class);
    private int targetPortfolioSize = 0;

    private int sumMinimumShares() {
        return minimumShares.values().stream().reduce(0, (a, b) -> a + b);
    }

    public void setTargetPortfolioSize(final int size) {
        PortfolioStructure.LOGGER.debug("Target portfolio size is {},- CZK.", size);
        this.targetPortfolioSize = size;
    }

    public int getTargetPortfolioSize() {
        return targetPortfolioSize;
    }

    public void addItem(final PortfolioStructureItem item) {
        minimumShares.put(item.getRating(), item.getMininumShareInPercent());
        maximumShares.put(item.getRating(), item.getMaximumShareInPercent());
        PortfolioStructure.LOGGER.debug("Target portfolio share for rating {} set between {} and {} %.",
                item.getRating(), item.getMininumShareInPercent(), item.getMaximumShareInPercent());
        if (this.sumMinimumShares() > 100) {
            throw new IllegalStateException("Minimum share of ratings has exceeded 100 %.");
        }
    }

    public int getMinimumShare(final Rating rating) {
        if (minimumShares.containsKey(rating)) {
            return minimumShares.get(rating);
        } else { // no minimum share specified; average the minimum share based on number of all unspecified ratings
            final int providedRatingCount = minimumShares.size();
            final int remainingShare = 100 - this.sumMinimumShares();
            return remainingShare / providedRatingCount;
        }
    }

    public int getMaximumShare(final Rating rating) {
        if (maximumShares.containsKey(rating)) {
            return maximumShares.get(rating);
        } else { // no maximum share specified; calculate minimum share and use it as maximum too
            return this.getMinimumShare(rating);
        }
    }

}
