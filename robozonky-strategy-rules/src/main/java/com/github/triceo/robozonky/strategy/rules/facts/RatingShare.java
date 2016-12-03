/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy.rules.facts;

import java.math.BigDecimal;

import com.github.triceo.robozonky.api.remote.enums.Rating;

public class RatingShare {

    private final Rating rating;
    private final double share;

    public RatingShare(final Rating r, final BigDecimal share) {
        this.rating = r;
        this.share = share.doubleValue();
    }

    public Rating getRating() {
        return rating;
    }

    public double getShare() {
        return share;
    }
}
