/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.api.remote.enums;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LoanTermIntervalTest {

    @Test
    void ranges() {
        assertThat(LoanTermInterval.values()[0].getMinInclusive()).isEqualTo(1);
        for (int i = 1; i < LoanTermInterval.values().length - 1; i++) {
            final LoanTermInterval current = LoanTermInterval.values()[i];
            final int currentEnd = current.getMinInclusive();
            final int previousEnd = LoanTermInterval.values()[i - 1].getMaxInclusive();
            assertThat(currentEnd).isEqualTo(previousEnd + 1)
                .as(current + " does not start where previous ends.");
        }
        assertThat(LoanTermInterval.values()[LoanTermInterval.values().length - 1].getMaxInclusive()).isEqualTo(84);
    }
}
