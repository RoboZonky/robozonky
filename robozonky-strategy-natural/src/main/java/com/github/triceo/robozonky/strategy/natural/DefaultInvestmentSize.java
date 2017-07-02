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

public class DefaultInvestmentSize {

    private final BigInteger minimumInvestmentInCzk, maximumInvestmentInCzk;

    public DefaultInvestmentSize(final BigInteger maximumInvestmentInCzk) {
        this(BigInteger.valueOf(200), maximumInvestmentInCzk);
    }

    public DefaultInvestmentSize(final BigInteger minimumInvestmentInCzk, final BigInteger maximumInvestmentInCzk) {
        this.minimumInvestmentInCzk = minimumInvestmentInCzk.min(maximumInvestmentInCzk);
        this.maximumInvestmentInCzk = maximumInvestmentInCzk.max(minimumInvestmentInCzk);
    }

    public BigInteger getMinimumInvestmentInCzk() {
        return minimumInvestmentInCzk;
    }

    public BigInteger getMaximumInvestmentInCzk() {
        return maximumInvestmentInCzk;
    }
}
