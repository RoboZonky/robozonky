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

package com.github.robozonky.app.util;

import java.math.BigDecimal;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ApiUtilTest extends AbstractZonkyLeveragingTest {

    @Test
    void getBalancePropertyInDryRun() {
        final int value = 0;
        System.setProperty("robozonky.default.dry_run_balance", String.valueOf(value));
        assertThat(ApiUtil.getDryRunBalance(null).intValue()).isEqualTo(value);
    }

    @Test
    void getLiveBalanceInDryRun() {
        final int value = -1;
        System.setProperty("robozonky.default.dry_run_balance", String.valueOf(value));
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(0);
        assertThat(ApiUtil.getDryRunBalance(z)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getBalancePropertyIgnoredWhenNotDryRun() {
        System.setProperty("robozonky.default.dry_run_balance", "200");
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(0);
        assertThat(ApiUtil.getLiveBalance(z)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getRemoteBalanceInDryRun() {
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(0);
        assertThat(ApiUtil.getDryRunBalance(z)).isEqualTo(BigDecimal.ZERO);
    }
}
