/*
 * Copyright 2018 The RoboZonky Project
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

import com.github.robozonky.api.remote.enums.Rating;

class PortfolioShare {

    private static void assertIsInRange(final int percentage) {
        if ((percentage < 0) || (percentage > 100)) {
            throw new IllegalArgumentException("Portfolio share must be in range of <0; 100>.");
        }
    }

    private final int mininumShareInPercent, maximumShareInPercent;
    private final Rating rating;

    public PortfolioShare(final Rating r, final int min, final int max) {
        this.rating = r;
        PortfolioShare.assertIsInRange(min);
        this.mininumShareInPercent = Math.min(min, max);
        PortfolioShare.assertIsInRange(max);
        this.maximumShareInPercent = Math.max(min, max);
    }

    public PortfolioShare(final Rating r, final int max) {
        this(r, max, max);
    }

    public int getMininumShareInPercent() {
        return mininumShareInPercent;
    }

    public int getMaximumShareInPercent() {
        return maximumShareInPercent;
    }

    public Rating getRating() {
        return rating;
    }

    @Override
    public String toString() {
        return "PortfolioShare{" +
                "mininumShareInPercent=" + mininumShareInPercent +
                ", maximumShareInPercent=" + maximumShareInPercent +
                ", rating=" + rating +
                '}';
    }
}
