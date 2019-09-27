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

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.Descriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;

import java.util.function.Supplier;

abstract class AbstractLoanWrapper<T extends Descriptor<?, ?, ?>> extends AbstractWrapper<T> {

    private final Supplier<Loan> loan;

    protected AbstractLoanWrapper(final T original, final PortfolioOverview portfolioOverview) {
        super(original, portfolioOverview);
        this.loan = original::related;
    }

    protected Loan getLoan() {
        return loan.get();
    }

    @Override
    public Region getRegion() {
        return getLoan().getRegion();
    }

    @Override
    public String getStory() {
        return getLoan().getStory();
    }
}
