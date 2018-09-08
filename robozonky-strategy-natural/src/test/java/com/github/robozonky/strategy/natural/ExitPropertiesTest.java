/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.strategy.natural;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExitPropertiesTest {

    @Test
    void newYearsEve() {
        final ExitProperties e = new ExitProperties(LocalDate.of(2021, 12, 31));
        assertThat(e.getAccountTermination()).isEqualTo(LocalDate.of(2021, 12, 31));
        assertThat(e.getSelloffStart()).isEqualTo(LocalDate.of(2021, 9, 30));
    }

    @Test
    void newYear() {
        final ExitProperties e = new ExitProperties(LocalDate.of(2022, 1, 1));
        assertThat(e.getAccountTermination()).isEqualTo(LocalDate.of(2022, 1, 1));
        assertThat(e.getSelloffStart()).isEqualTo(LocalDate.of(2021, 10, 1));
    }

}
