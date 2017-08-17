/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.triceo.robozonky.app.util;

import java.math.BigDecimal;

import com.github.triceo.robozonky.app.investing.AbstractInvestingTest;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ApiUtilTest extends AbstractInvestingTest {

    @Test
    public void getBalancePropertyInDryRun() {
        final int value = 0;
        System.setProperty("robozonky.default.dry_run_balance", String.valueOf(value));
        Assertions.assertThat(ApiUtil.getDryRunBalance(null).intValue()).isEqualTo(value);
    }

    @Test
    public void getLiveBalanceInDryRun() {
        final int value = -1;
        System.setProperty("robozonky.default.dry_run_balance", String.valueOf(value));
        final Zonky z = AbstractInvestingTest.harmlessZonky(0);
        Assertions.assertThat(ApiUtil.getDryRunBalance(z)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void getBalancePropertyIgnoredWhenNotDryRun() {
        System.setProperty("robozonky.default.dry_run_balance", "200");
        final Zonky z = AbstractInvestingTest.harmlessZonky(0);
        Assertions.assertThat(ApiUtil.getLiveBalance(z)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void getRemoteBalanceInDryRun() {
        final Zonky z = AbstractInvestingTest.harmlessZonky(0);
        Assertions.assertThat(ApiUtil.getDryRunBalance(z)).isEqualTo(BigDecimal.ZERO);
    }

}
