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

public class InvestmentSize extends DefaultInvestmentSize {

    private final Rating rating;

    public InvestmentSize(final Rating r, final int min, final int max) {
        super(min, max);
        this.rating = r;
    }

    public InvestmentSize(final Rating r, final int max) {
        super(max);
        this.rating = r;
    }

    public InvestmentSize(final Rating r, final DefaultInvestmentSize def) {
        super(def.getMinimumInvestmentInCzk(), def.getMaximumInvestmentInCzk());
        this.rating = r;
    }

    public Rating getRating() {
        return rating;
    }
}
