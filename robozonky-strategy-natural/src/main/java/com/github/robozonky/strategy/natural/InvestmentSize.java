/*
 * Copyright 2017 The RoboZonky Project
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

import com.github.robozonky.internal.api.Defaults;

public class InvestmentSize {

    private final int minimumInvestmentInCzk, maximumInvestmentInCzk;

    public InvestmentSize() {
        this(Defaults.MAXIMUM_INVESTMENT_IN_CZK);
    }

    public InvestmentSize(final int maximumInvestmentInCzk) {
        this(0, maximumInvestmentInCzk);
    }

    public InvestmentSize(final int minimumInvestmentInCzk, final int maximumInvestmentInCzk) {
        this.minimumInvestmentInCzk = Math.min(minimumInvestmentInCzk, maximumInvestmentInCzk);
        this.maximumInvestmentInCzk = Math.max(minimumInvestmentInCzk, maximumInvestmentInCzk);
        if (this.maximumInvestmentInCzk > Defaults.MAXIMUM_INVESTMENT_IN_CZK) {
            throw new IllegalStateException("Maximum investment can not be more than 5000 CZK.");
        }
    }

    public int getMinimumInvestmentInCzk() {
        return minimumInvestmentInCzk;
    }

    public int getMaximumInvestmentInCzk() {
        return maximumInvestmentInCzk;
    }

    @Override
    public String toString() {
        return "DefaultInvestmentSize{" +
                "minimumInvestmentInCzk=" + minimumInvestmentInCzk +
                ", maximumInvestmentInCzk=" + maximumInvestmentInCzk +
                '}';
    }
}
