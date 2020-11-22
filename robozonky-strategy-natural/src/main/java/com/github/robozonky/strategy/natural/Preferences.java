/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.strategy.natural;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class Preferences {

    private static final Logger LOGGER = LogManager.getLogger(Preferences.class);
    private static final AtomicReference<Preferences> INSTANCE = new AtomicReference<>();

    private final PortfolioOverview referencePortfolio;
    private final Set<Rating> desirableRatings;

    private Preferences(PortfolioOverview portfolio, Set<Rating> desirableRatings) {
        this.referencePortfolio = portfolio;
        this.desirableRatings = Collections.unmodifiableSet(desirableRatings);
    }

    public static Preferences get(ParsedStrategy strategy, PortfolioOverview portfolio) {
        return INSTANCE.updateAndGet(old -> {
            if (old != null && Objects.equals(old.referencePortfolio, portfolio)) {
                LOGGER.trace("Reusing {} for {}.", old, portfolio);
                return old;
            }
            LOGGER.debug("Created new instance for {}.", portfolio);
            return new Preferences(portfolio, Util.getRatingsInDemand(strategy, portfolio));
        });
    }

    public Set<Rating> getDesirableRatings() {
        return desirableRatings;
    }

}
