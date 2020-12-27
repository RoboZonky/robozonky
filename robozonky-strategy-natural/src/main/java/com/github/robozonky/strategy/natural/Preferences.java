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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.util.functional.Memoizer;

final class Preferences {

    private static final Logger LOGGER = LogManager.getLogger(Preferences.class);
    private static final AtomicReference<Preferences> INSTANCE = new AtomicReference<>();

    private final ParsedStrategy referenceStrategy;
    private final PortfolioOverview referencePortfolio;
    private final Function<Ratio, Boolean> ratingDemand;

    private Preferences(ParsedStrategy strategy, PortfolioOverview portfolio, Predicate<Ratio> ratingDemandPredicate) {
        this.referenceStrategy = strategy;
        this.referencePortfolio = portfolio;
        this.ratingDemand = Memoizer.memoize(ratingDemandPredicate::test);
    }

    public static Preferences get(ParsedStrategy strategy, PortfolioOverview portfolio) {
        return INSTANCE.updateAndGet(old -> {
            if (old != null && Objects.equals(old.referenceStrategy, strategy) &&
                    Objects.equals(old.referencePortfolio, portfolio)) {
                LOGGER.trace("Reusing {} for {}.", old, portfolio);
                return old;
            }
            LOGGER.debug("Created new instance for {}.", portfolio);
            return new Preferences(strategy, portfolio, rating -> isDesirable(rating, strategy, portfolio));
        });
    }

    private static boolean isDesirable(Ratio rating, ParsedStrategy strategy, PortfolioOverview portfolioOverview) {
        var permittedShare = strategy.getPermittedShare(rating);
        if (permittedShare.compareTo(Ratio.ZERO) <= 0) {
            Audit.LOGGER.debug("Rating {} is not permitted.", rating);
            return false;
        }
        var currentRatingShare = portfolioOverview.getShareOnInvestment(Rating.findByInterestRate(rating));
        var overinvested = currentRatingShare.compareTo(permittedShare) >= 0;
        if (overinvested) { // we over-invested into this rating; do not include
            Audit.LOGGER.debug("Rating {} over-invested. (Expected {}, got {}.)", rating, permittedShare,
                    currentRatingShare);
            return false;
        }
        Audit.LOGGER.debug("Rating {} under-invested. (Expected {}, got {}.)", rating, permittedShare,
                currentRatingShare);
        return true;
    }

    public boolean isDesirable(Ratio rating) {
        return ratingDemand.apply(rating);
    }

}
