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

import java.util.Optional;

import com.github.triceo.robozonky.strategy.InvestmentStrategy;

class AppContext {

    private InvestmentStrategy investmentStrategy = null;
    private final String username, password;
    private final OperatingMode operatingMode;
    private boolean isDryRun = false;
    private int dryRunBalance = -1, loanId = -1, loanAmount = -1;

    public AppContext(final String username, final String password, final InvestmentStrategy investmentStrategy,
                      final Optional<Integer> dryRunBalance) {
        this(username, password, investmentStrategy);
        this.dryRunBalance = dryRunBalance.isPresent() ? dryRunBalance.get() : -1;
        this.isDryRun = true;
    }

    public AppContext(final String username, final String password, final InvestmentStrategy investmentStrategy) {
        this.operatingMode = OperatingMode.STRATEGY_DRIVEN;
        this.investmentStrategy = investmentStrategy;
        this.username = username;
        this.password = password;
    }

    public OperatingMode getOperatingMode() {
        return operatingMode;
    }

    public AppContext(final String username, final String password, final int loanId, final int loanAmount,
                      final Optional<Integer> dryRunBalance) {
        this(username, password, loanId, loanAmount);
        this.dryRunBalance = dryRunBalance.isPresent() ? dryRunBalance.get() : -1;
        this.isDryRun = true;
    }

    public AppContext(final String username, final String password, final int loanId, final int loanAmount) {
        this.operatingMode = OperatingMode.USER_DRIVER;
        this.username = username;
        this.password = password;
        this.loanId = loanId;
        this.loanAmount = loanAmount;
    }

    public int getLoanId() {
        return loanId;
    }

    public int getLoanAmount() {
        return loanAmount;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
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
