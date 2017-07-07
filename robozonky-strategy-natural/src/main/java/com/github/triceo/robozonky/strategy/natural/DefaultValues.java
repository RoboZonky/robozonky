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

import com.github.triceo.robozonky.api.remote.entities.Loan;

public class DefaultValues {

    private int targetPortfolioSize;
    private DefaultInvestmentSize investmentSize;
    private MarketplaceFilterCondition confirmationCondition;

    public int getTargetPortfolioSize() {
        return targetPortfolioSize;
    }

    public void setTargetPortfolioSize(final int targetPortfolioSize) {
        this.targetPortfolioSize = targetPortfolioSize;
    }

    public DefaultInvestmentSize getInvestmentSize() {
        return investmentSize;
    }

    public void setInvestmentSize(final DefaultInvestmentSize investmentSize) {
        this.investmentSize = investmentSize;
    }

    public boolean needsConfirmation(final Loan loan) {
        return confirmationCondition.test(loan);
    }

    public void setConfirmationCondition(final MarketplaceFilterCondition confirmationCondition) {
        this.confirmationCondition = confirmationCondition;
    }
}
