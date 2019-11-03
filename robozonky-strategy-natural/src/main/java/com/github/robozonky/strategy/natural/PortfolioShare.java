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

package com.github.robozonky.strategy.natural;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class PortfolioShare {

    private static final Logger LOGGER = LogManager.getLogger(PortfolioShare.class);

    private final Ratio permitted;
    private final Rating rating;

    public PortfolioShare(final Rating r, final Ratio min, final Ratio max) {
        this.rating = r;
        PortfolioShare.assertIsInRange(min);
        PortfolioShare.assertIsInRange(max);
        this.permitted = min.max(max);
        if (min.compareTo(max) != 0) {
            LOGGER.warn("Portfolio share for rating {} gives a range of <{}; {}>. " +
                    "The minimum has been deprecated and will be ignored. Please update your strategy.", r, min, max);
        }
    }

    public PortfolioShare(final Rating r, final Ratio max) {
        this(r, max, max);
    }

    private static void assertIsInRange(final Ratio ratio) {
        final double actual = ratio.asPercentage().doubleValue();
        if (actual < 0 || actual > 100) {
            throw new IllegalArgumentException("Portfolio share must be in range of <0; 100>.");
        }
    }

    public Ratio getPermitted() {
        return permitted;
    }

    public Rating getRating() {
        return rating;
    }

    @Override
    public String toString() {
        return "PortfolioShare{" +
                "maximum=" + permitted +
                ", rating=" + rating +
                '}';
    }
}
