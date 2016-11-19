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

package com.github.triceo.robozonky.app.configuration;

import java.util.Optional;
import java.util.OptionalInt;

import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;

public class Configuration {

    private InvestmentStrategy investmentStrategy = null;
    private final boolean isDryRun;
    private int dryRunBalance = -1, loanId = -1, loanAmount = -1;
    private final int captchaDelayInSeconds, sleepPeriodInMinutes;

    public Configuration(final int loanId, final int loanAmount, final int sleepPeriodInMinutes,
                         final int captchaDelayInSeconds) {
        this.loanId = loanId;
        this.loanAmount = loanAmount;
        this.captchaDelayInSeconds = captchaDelayInSeconds;
        this.sleepPeriodInMinutes = sleepPeriodInMinutes;
        this.isDryRun = false;
    }

    public Configuration(final int loanId, final int loanAmount, final int sleepPeriodInMinutes,
                         final int captchaDelayInSeconds, final int dryRunBalance) {
        this.loanId = loanId;
        this.loanAmount = loanAmount;
        this.captchaDelayInSeconds = captchaDelayInSeconds;
        this.sleepPeriodInMinutes = sleepPeriodInMinutes;
        this.dryRunBalance = dryRunBalance;
        this.isDryRun = true;
    }

    public Configuration(final InvestmentStrategy investmentStrategy, final int sleepPeriodInMinutes,
                         final int captchaDelayInSeconds) {
        this.investmentStrategy = investmentStrategy;
        this.captchaDelayInSeconds = captchaDelayInSeconds;
        this.sleepPeriodInMinutes = sleepPeriodInMinutes;
        this.isDryRun = false;
    }

    public Configuration(final InvestmentStrategy investmentStrategy, final int sleepPeriodInMinutes,
                         final int captchaDelayInSeconds, final int dryRunBalance) {
        this.investmentStrategy = investmentStrategy;
        this.captchaDelayInSeconds = captchaDelayInSeconds;
        this.dryRunBalance = dryRunBalance;
        this.sleepPeriodInMinutes = sleepPeriodInMinutes;
        this.isDryRun = true;
    }

    public int getCaptchaDelayInSeconds() {
        return captchaDelayInSeconds;
    }

    public int getSleepPeriodInMinutes() {
        return sleepPeriodInMinutes;
    }

    public OptionalInt getLoanId() {
        return loanId < 1 ? OptionalInt.empty() : OptionalInt.of(loanId);
    }

    public OptionalInt getLoanAmount() {
        return loanAmount < 0 ? OptionalInt.empty() : OptionalInt.of(loanAmount);
    }

    public boolean isDryRun() {
        return isDryRun;
    }

    public OptionalInt getDryRunBalance() {
        return dryRunBalance < 0 ? OptionalInt.empty() : OptionalInt.of(dryRunBalance);
    }

    public Optional<InvestmentStrategy> getInvestmentStrategy() {
        return investmentStrategy == null ? Optional.empty() : Optional.of(investmentStrategy);
    }
}
