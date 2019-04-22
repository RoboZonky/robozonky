/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.app.summaries;

import java.math.BigDecimal;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class CashFlowTest {

    @Test
    void fee() {
        final CashFlow cf = CashFlow.fee(BigDecimal.ONE);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cf.getType()).isEqualTo(CashFlow.Type.FEE);
            softly.assertThat(cf.getAmount()).isEqualTo(BigDecimal.ONE);
        });
    }

    @Test
    void investment() {
        final CashFlow cf = CashFlow.investment(BigDecimal.ONE);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cf.getType()).isEqualTo(CashFlow.Type.INVESTMENT);
            softly.assertThat(cf.getAmount()).isEqualTo(BigDecimal.ONE);
        });
    }

    @Test
    void external() {
        final CashFlow cf = CashFlow.external(BigDecimal.ONE);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cf.getType()).isEqualTo(CashFlow.Type.EXTERNAL);
            softly.assertThat(cf.getAmount()).isEqualTo(BigDecimal.ONE);
        });
    }
}
