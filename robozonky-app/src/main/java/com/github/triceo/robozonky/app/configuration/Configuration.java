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

package com.github.triceo.robozonky.app.configuration;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.Optional;
import java.util.OptionalInt;

import com.github.triceo.robozonky.ZonkyProxy;
import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;

public class Configuration {

    private Refreshable<InvestmentStrategy> investmentStrategy = null;
    private final ZonkyProxy.Builder zonkyProxyBuilder;
    private final boolean isDryRun, isFaultTolerant;
    private int loanId = -1, loanAmount = -1;
    private final int captchaDelayInSeconds = Defaults.getCaptchaDelayInSeconds(),
            dryRunBalance = Defaults.getDefaultDryRunBalance();
    private final TemporalAmount sleepPeriod;
    private final AuthenticationHandler authenticationHandler;

    public Configuration(final int loanId, final int loanAmount, final AuthenticationHandler auth,
                         final ZonkyProxy.Builder builder, final boolean faultTolerant, final boolean dryRun) {
        this.loanId = loanId;
        this.loanAmount = loanAmount;
        this.authenticationHandler = auth;
        this.isDryRun = dryRun;
        this.isFaultTolerant = faultTolerant;
        this.zonkyProxyBuilder = builder;
        this.sleepPeriod = Duration.ZERO;
    }

    public Configuration(final Refreshable<InvestmentStrategy> investmentStrategy, final AuthenticationHandler auth,
                         final ZonkyProxy.Builder builder, final int sleepPeriodInMinutes, final boolean faultTolerant, final boolean dryRun) {
        this.zonkyProxyBuilder = builder;
        this.investmentStrategy = investmentStrategy;
        this.authenticationHandler = auth;
        this.sleepPeriod = Duration.ofMinutes(sleepPeriodInMinutes);
        this.isDryRun = dryRun;
        this.isFaultTolerant = faultTolerant;
    }

    public AuthenticationHandler getAuthenticationHandler() {
        return authenticationHandler;
    }

    public TemporalAmount getCaptchaDelay() {
        return Duration.ofSeconds(captchaDelayInSeconds);
    }

    public TemporalAmount getSleepPeriod() {
        return this.sleepPeriod;
    }

    public OptionalInt getLoanId() {
        return loanId < 1 ? OptionalInt.empty() : OptionalInt.of(loanId);
    }

    public OptionalInt getLoanAmount() {
        return loanAmount < Defaults.MINIMUM_INVESTMENT_IN_CZK ? OptionalInt.empty() : OptionalInt.of(loanAmount);
    }

    public boolean isFaultTolerant() {
        return isFaultTolerant;
    }

    public boolean isDryRun() {
        return isDryRun;
    }

    public OptionalInt getDryRunBalance() {
        return (isDryRun && dryRunBalance >= 0) ? OptionalInt.of(dryRunBalance) : OptionalInt.empty();
    }

    public ZonkyProxy.Builder getZonkyProxyBuilder() {
        return zonkyProxyBuilder;
    }

    public Optional<Refreshable<InvestmentStrategy>> getInvestmentStrategy() {
        return Optional.ofNullable(investmentStrategy);
    }
}
