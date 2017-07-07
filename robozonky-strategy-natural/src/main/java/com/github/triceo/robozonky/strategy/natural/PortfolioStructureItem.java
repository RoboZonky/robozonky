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

import com.github.triceo.robozonky.api.remote.enums.Rating;

class PortfolioStructureItem {

    private final int mininumShareInPercent, maximumShareInPercent;
    private final Rating rating;

    public PortfolioStructureItem(final Rating r, final int min, final int max) {
        this.rating = r;
        this.mininumShareInPercent = Math.min(min, max);
        this.maximumShareInPercent = Math.max(min, max);
    }

    public PortfolioStructureItem(final Rating r, final int max) {
        this(r, 0, max);
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

}
