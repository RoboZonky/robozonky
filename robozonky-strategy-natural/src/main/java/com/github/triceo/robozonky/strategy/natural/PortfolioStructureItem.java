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

import com.github.triceo.robozonky.api.remote.enums.Rating;

public class PortfolioStructureItem {

    private final BigInteger mininumShareInPercent, maximumShareInPercent;
    private final Rating rating;

    public PortfolioStructureItem(final Rating r, final BigInteger min, final BigInteger max) {
        this.rating = r;
        this.mininumShareInPercent = min.min(max);
        this.maximumShareInPercent = min.max(max);
    }

    public PortfolioStructureItem(final Rating r, final BigInteger max) {
        this(r, BigInteger.ZERO, max);
    }

    public BigInteger getMininumShareInPercent() {
        return mininumShareInPercent;
    }

    public BigInteger getMaximumShareInPercent() {
        return maximumShareInPercent;
    }

    public Rating getRating() {
        return rating;
    }

}
