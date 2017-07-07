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
import com.github.triceo.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultValues {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultValues.class);

    private final DefaultPortfolio portfolio;
    private int targetPortfolioSize = Integer.MAX_VALUE, minimumBalance = Defaults.MINIMUM_INVESTMENT_IN_CZK;
    private DefaultInvestmentSize investmentSize = new DefaultInvestmentSize();
    private MarketplaceFilterCondition confirmationCondition = new MarketplaceFilterCondition() {
        // by default, do not confirm anything ever
    };

    public DefaultValues(final DefaultPortfolio portfolio) {
        this.portfolio = portfolio;
    }

    public DefaultPortfolio getPortfolio() {
        return portfolio;
    }

    public int getMinimumBalance() {
        return minimumBalance;
    }

    public void setMinimumBalance(final int minimumBalance) {
        if (minimumBalance < Defaults.MINIMUM_INVESTMENT_IN_CZK) {
            throw new IllegalArgumentException("Minimum balance must be at least "
                    + Defaults.MINIMUM_INVESTMENT_IN_CZK + "CZK.");
        }
        this.minimumBalance = minimumBalance;
    }

    public int getTargetPortfolioSize() {
        return targetPortfolioSize;
    }

    public void setTargetPortfolioSize(final int targetPortfolioSize) {
        if (targetPortfolioSize <= 0) {
            throw new IllegalArgumentException("Target portfolio size must be a positive number.");
        }
        DefaultValues.LOGGER.debug("Target portfolio size set to {} CZK.", targetPortfolioSize);
        this.targetPortfolioSize = targetPortfolioSize;
    }

    public DefaultInvestmentSize getInvestmentSize() {
        return investmentSize;
    }

    public void setInvestmentSize(final DefaultInvestmentSize investmentSize) {
        if (investmentSize == null) {
            throw new IllegalArgumentException("Default investment size must be provided.");
        }
        DefaultValues.LOGGER.debug("Investment size set between {} and {} CZK.",
                investmentSize.getMinimumInvestmentInCzk(), investmentSize.getMaximumInvestmentInCzk());
        this.investmentSize = investmentSize;
    }

    public boolean needsConfirmation(final Loan loan) {
        return confirmationCondition.test(loan);
    }

    public void setConfirmationCondition(final MarketplaceFilterCondition confirmationCondition) {
        if (confirmationCondition == null) {
            throw new IllegalArgumentException("Confirmation condition must be provided.");
        }
        this.confirmationCondition = confirmationCondition;
    }
}
