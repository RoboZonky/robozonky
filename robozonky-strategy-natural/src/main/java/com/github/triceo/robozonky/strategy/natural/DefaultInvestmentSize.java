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

import com.github.triceo.robozonky.internal.api.Defaults;

class DefaultInvestmentSize {

    private final int minimumInvestmentInCzk, maximumInvestmentInCzk;

    public DefaultInvestmentSize() {
        this(Defaults.MINIMUM_INVESTMENT_IN_CZK);
    }

    public DefaultInvestmentSize(final int maximumInvestmentInCzk) {
        this(Defaults.MINIMUM_INVESTMENT_IN_CZK, maximumInvestmentInCzk);
    }

    public DefaultInvestmentSize(final int minimumInvestmentInCzk, final int maximumInvestmentInCzk) {
        this.minimumInvestmentInCzk = Math.min(minimumInvestmentInCzk, maximumInvestmentInCzk);
        this.maximumInvestmentInCzk = Math.max(minimumInvestmentInCzk, maximumInvestmentInCzk);
    }

    public int getMinimumInvestmentInCzk() {
        return minimumInvestmentInCzk;
    }

    public int getMaximumInvestmentInCzk() {
        return maximumInvestmentInCzk;
    }
}
