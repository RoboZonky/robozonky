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

package com.github.triceo.robozonky.app.investing;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.Wallet;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.mockito.Mockito;

public class StrategyExecutionTest extends AbstractInvestingTest {

    @Rule
    public final RestoreSystemProperties propertyRestore = new RestoreSystemProperties();

    @Test
    public void getBalancePropertyInDryRun() {
        final int value = 200;
        System.setProperty("robozonky.default.dry_run_balance", String.valueOf(value));
        final ZonkyProxy.Builder p = new ZonkyProxy.Builder().asDryRun();
        Assertions.assertThat(StrategyExecution.getAvailableBalance(p.build(null)).intValue()).isEqualTo(value);
    }

    @Test
    public void getBalancePropertyIgnoredWhenNotDryRun() {
        System.setProperty("robozonky.default.dry_run_balance", "200");
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getWallet()).thenReturn(new Wallet(1, 2, BigDecimal.TEN, BigDecimal.ZERO));
        final ZonkyProxy.Builder p = new ZonkyProxy.Builder();
        Assertions.assertThat(StrategyExecution.getAvailableBalance(p.build(api))).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void getRemoteBalanceInDryRun() {
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getWallet()).thenReturn(new Wallet(1, 2, BigDecimal.TEN, BigDecimal.ZERO));
        final ZonkyProxy.Builder p = new ZonkyProxy.Builder().asDryRun();
        Assertions.assertThat(StrategyExecution.getAvailableBalance(p.build(api))).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void empty() {
        final StrategyExecution exec = new StrategyExecution(null, null, null, null);
        Assertions.assertThat(exec.apply(Collections.emptyList())).isEmpty();
        // check events
        final List<Event> events = this.getNewEvents();
        Assertions.assertThat(events).isEmpty();
    }

}
