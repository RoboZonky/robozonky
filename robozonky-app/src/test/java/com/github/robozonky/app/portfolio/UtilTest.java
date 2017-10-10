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

package com.github.robozonky.app.portfolio;

import java.time.Duration;
import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class UtilTest {

    @Test
    public void yesterday() {
        final LocalDate today = LocalDate.now();
        final LocalDate yesterday = today.minusDays(1);
        Assertions.assertThat(Util.getYesterdayIfAfter(Duration.ZERO)).isEqualTo(today);
        Assertions.assertThat(Util.getYesterdayIfAfter(Duration.ofHours(24))).isEqualTo(yesterday);
    }

}
