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
package com.github.triceo.robozonky.app;

import com.github.triceo.robozonky.strategy.InvestmentStrategy;

class AppContext {

    private InvestmentStrategy investmentStrategy = null;
    private final OperatingMode operatingMode;
    private final boolean isDryRun;
    private int dryRunBalance = -1, loanId = -1, loanAmount = -1;
    private final int captchaDelayInSeconds;

    public AppContext(final int loanId, final int loanAmount, final int captchaDelayInSeconds) {
        this.operatingMode = OperatingMode.USER_DRIVEN;
        this.loanId = loanId;
        this.loanAmount = loanAmount;
        this.captchaDelayInSeconds = captchaDelayInSeconds;
        this.isDryRun = false;
    }

    public AppContext(final int loanId, final int loanAmount, final int captchaDelayInSeconds,
                      final int dryRunBalance) {
        this.operatingMode = OperatingMode.USER_DRIVEN;
        this.loanId = loanId;
        this.loanAmount = loanAmount;
        this.captchaDelayInSeconds = captchaDelayInSeconds;
        this.dryRunBalance = dryRunBalance;
        this.isDryRun = true;
    }

    public AppContext(final InvestmentStrategy investmentStrategy, final int captchaDelayInSeconds) {
        this.operatingMode = OperatingMode.STRATEGY_DRIVEN;
        this.investmentStrategy = investmentStrategy;
        this.captchaDelayInSeconds = captchaDelayInSeconds;
        this.isDryRun = false;
    }

    public AppContext(final InvestmentStrategy investmentStrategy, final int captchaDelayInSeconds,
                      final int dryRunBalance) {
        this.operatingMode = OperatingMode.STRATEGY_DRIVEN;
        this.investmentStrategy = investmentStrategy;
        this.captchaDelayInSeconds = captchaDelayInSeconds;
        this.dryRunBalance = dryRunBalance;
        this.isDryRun = true;
    }

    public int getCaptchaDelayInSeconds() {
        return captchaDelayInSeconds;
    }

    public OperatingMode getOperatingMode() {
        return operatingMode;
    }

    public int getLoanId() {
        return loanId;
    }

    public int getLoanAmount() {
        return loanAmount;
    }

    public boolean isDryRun() {
        return isDryRun;
    }

    public int getDryRunBalance() {
        return dryRunBalance;
    }

    public InvestmentStrategy getInvestmentStrategy() {
        return investmentStrategy;
    }
}
