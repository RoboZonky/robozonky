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

package com.github.robozonky.strategy.natural.conditions;

import java.time.LocalDate;

import com.github.robozonky.strategy.natural.ExitProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ExitPropertiesTest {

    @Test
    void defaults() {
        final LocalDate now = LocalDate.now();
        final ExitProperties e = new ExitProperties(now);
        assertSoftly(softly -> {
            e.getAccountTermination().isEqual(now);
            e.getSelloffStart().isEqual(now.minusMonths(3));
        });
    }

    @Test
    void customSelloff() {
        final LocalDate now = LocalDate.now();
        final LocalDate selloff = LocalDate.now().minusDays(1);
        final ExitProperties e = new ExitProperties(now, selloff);
        assertSoftly(softly -> {
            e.getAccountTermination().isEqual(now);
            e.getSelloffStart().isEqual(selloff);
        });
    }

    @Test
    void invalidSelloff() {
        final LocalDate now = LocalDate.now();
        assertThatThrownBy(() -> new ExitProperties(now, now))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
