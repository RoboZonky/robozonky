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

package com.github.robozonky.app.tenant;

import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DivisorTest extends AbstractRoboZonkyTest {

    @Test
    void calculate() {
        Divisor d = new Divisor(2);
        assertThat(d.getSharePerMille()).isEqualTo(0);
        d.add(1);
        assertThat(d.getSharePerMille()).isEqualTo(500);
        d.add(10);
        assertThat(d.getSharePerMille()).isEqualTo(5500);
    }

    @Test
    void calculateWithZeroBase() {
        final Divisor d = new Divisor(0);
        assertThat(d.getSharePerMille()).isEqualTo(Long.MAX_VALUE);
        d.add(1);
        assertThat(d.getSharePerMille()).isEqualTo(Long.MAX_VALUE);
    }

}
