/*
 * Copyright 2018 The RoboZonky Project
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

import java.time.LocalDate;
import java.time.Period;

import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilterCondition;

class DefaultValues {

    private final DefaultPortfolio portfolio;
    private long targetPortfolioSize = Long.MAX_VALUE;
    private long minimumBalance = 0;
    private InvestmentSize investmentSize = new InvestmentSize();
    private DefaultInvestmentShare investmentShare = new DefaultInvestmentShare();
    private ExitProperties exitProperties;
    private MarketplaceFilterCondition confirmationCondition = MarketplaceFilterCondition.neverAccepting();

    public DefaultValues(final DefaultPortfolio portfolio) {
        this.portfolio = portfolio;
    }

    public DefaultPortfolio getPortfolio() {
        return portfolio;
    }

    public long getMinimumBalance() {
        return minimumBalance;
    }

    public void setMinimumBalance(final long minimumBalance) {
        this.minimumBalance = minimumBalance;
    }

    public void setExitProperties(final ExitProperties properties) {
        this.exitProperties = properties;
    }

    public boolean isSelloffStarted() {
        if (exitProperties == null) {
            return false;
        } else {
            return exitProperties.getSelloffStart().isBefore(LocalDate.now());
        }
    }

    public long getMonthsBeforeExit() {
        if (exitProperties == null) {
            return -1;
        } else {
            return Math.max(0, Period.between(LocalDate.now(), exitProperties.getAccountTermination()).toTotalMonths());
        }
    }

    public long getTargetPortfolioSize() {
        return targetPortfolioSize;
    }

    public void setTargetPortfolioSize(final long targetPortfolioSize) {
        if (targetPortfolioSize <= 0) {
            throw new IllegalArgumentException("Target portfolio size must be a positive number.");
        }
        this.targetPortfolioSize = targetPortfolioSize;
    }

    public DefaultInvestmentShare getInvestmentShare() {
        return investmentShare;
    }

    public void setInvestmentShare(final DefaultInvestmentShare investmentShare) {
        this.investmentShare = investmentShare;
    }

    public InvestmentSize getInvestmentSize() {
        return investmentSize;
    }

    public void setInvestmentSize(final InvestmentSize investmentSize) {
        if (investmentSize == null) {
            throw new IllegalArgumentException("Default investment size must be provided.");
        }
        this.investmentSize = investmentSize;
    }

    public boolean needsConfirmation(final LoanDescriptor loan) {
        return confirmationCondition.test(Wrapper.wrap(loan));
    }

    public void setConfirmationCondition(final MarketplaceFilterCondition confirmationCondition) {
        if (confirmationCondition == null) {
            throw new IllegalArgumentException("Confirmation condition must be provided.");
        }
        this.confirmationCondition = confirmationCondition;
    }

    @Override
    public String toString() {
        return "DefaultValues{" +
                "portfolio=" + portfolio +
                ", targetPortfolioSize=" + targetPortfolioSize +
                ", minimumBalance=" + minimumBalance +
                ", investmentSize=" + investmentSize +
                ", investmentShare=" + investmentShare +
                ", exitProperties=" + exitProperties +
                ", confirmationCondition=" + confirmationCondition +
                '}';
    }
}
