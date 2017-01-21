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

import java.time.temporal.ChronoUnit;

import com.github.triceo.robozonky.ZonkyProxy;
import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class ConfigurationTest {

    @Test
    public void withLoan() {
        final int loanId = 1, loanAmount = Defaults.MINIMUM_INVESTMENT_IN_CZK;
        final ZonkyProxy.Builder builder = new ZonkyProxy.Builder();
        final Configuration cfg = new Configuration(loanId, loanAmount, null, builder, false, false);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cfg.getLoanId()).hasValue(loanId);
            softly.assertThat(cfg.getLoanAmount()).hasValue(loanAmount);
            softly.assertThat(cfg.getZonkyProxyBuilder()).isEqualTo(builder);
            softly.assertThat(cfg.getSleepPeriod().get(ChronoUnit.SECONDS)).isEqualTo(0);
            softly.assertThat(cfg.isDryRun()).isFalse();
            softly.assertThat(cfg.isFaultTolerant()).isFalse();
            softly.assertThat(cfg.getDryRunBalance()).isEmpty();
            softly.assertThat(cfg.getInvestmentStrategy()).isEmpty();
        });
    }

    @Test
    public void withLoanAndDry() {
        final int loanId = 1, loanAmount = Defaults.MINIMUM_INVESTMENT_IN_CZK;
        final ZonkyProxy.Builder builder = new ZonkyProxy.Builder();
        final Configuration cfg = new Configuration(loanId, loanAmount, null, builder, false, true);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cfg.getLoanId()).hasValue(loanId);
            softly.assertThat(cfg.getLoanAmount()).hasValue(loanAmount);
            softly.assertThat(cfg.getZonkyProxyBuilder()).isEqualTo(builder);
            softly.assertThat(cfg.getSleepPeriod().get(ChronoUnit.SECONDS)).isEqualTo(0);
            softly.assertThat(cfg.isDryRun()).isTrue();
            softly.assertThat(cfg.getDryRunBalance()).isEmpty();
            softly.assertThat(cfg.getInvestmentStrategy()).isEmpty();
            softly.assertThat(cfg.getZonkyProxyBuilder()).isNotNull();
        });
    }

    @Test
    public void withWrongLoanIdAndAmount() {
        final int loanId = -1, loanAmount = Defaults.MINIMUM_INVESTMENT_IN_CZK - 1;
        final Configuration cfg = new Configuration(loanId, loanAmount, null, null, false, true);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cfg.getLoanId()).isEmpty();
            softly.assertThat(cfg.getLoanAmount()).isEmpty();
        });
    }

    @Test
    public void withStrategy() {
        final int sleep = 60;
        final Refreshable<InvestmentStrategy> refreshable = Refreshable.createImmutable(null);
        final ZonkyProxy.Builder builder = new ZonkyProxy.Builder();
        final Configuration cfg = new Configuration(refreshable, null, builder, sleep, true, false);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cfg.getInvestmentStrategy()).hasValue(refreshable);
            softly.assertThat(cfg.getSleepPeriod().get(ChronoUnit.SECONDS)).isEqualTo(sleep * 60);
            softly.assertThat(cfg.getZonkyProxyBuilder()).isEqualTo(builder);
            softly.assertThat(cfg.isDryRun()).isFalse();
            softly.assertThat(cfg.isFaultTolerant()).isTrue();
            softly.assertThat(cfg.getDryRunBalance()).isEmpty();
            softly.assertThat(cfg.getLoanId()).isEmpty();
            softly.assertThat(cfg.getLoanAmount()).isEmpty();
        });
    }

}

