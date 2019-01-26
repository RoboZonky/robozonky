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

package com.github.robozonky.strategy.natural;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class RoboZonkyVersionTest {

    @Test
    void comparator() {
        final RoboZonkyVersion current = new RoboZonkyVersion("4.1.0");
        assertSoftly(softly -> {
            softly.assertThat(current).isGreaterThan(new RoboZonkyVersion("3.9.9"));
            softly.assertThat(current).isEqualByComparingTo(current);
            softly.assertThat(current).isLessThan(new RoboZonkyVersion("4.1.1"));
            softly.assertThat(current).isLessThan(new RoboZonkyVersion("4.2.0"));
            softly.assertThat(current).isLessThan(new RoboZonkyVersion("5.4.3"));
        });
    }

    @Test
    void snapshot() {
        final RoboZonkyVersion current = new RoboZonkyVersion((String) null);
        assertThat(current).isGreaterThan(new RoboZonkyVersion("999.999.999"));
    }

    @Test
    void equality() {
        final RoboZonkyVersion current = new RoboZonkyVersion("4.1.0");
        assertSoftly(softly -> {
            softly.assertThat(current).isEqualTo(current);
            softly.assertThat(current).isEqualTo(new RoboZonkyVersion("4.1.0"));
            softly.assertThat(current).isEqualTo(new RoboZonkyVersion("4.1.0-SNAPSHOT")); // SNAPSHOTs do not matter
            softly.assertThat(current).isEqualTo(new RoboZonkyVersion("4.1.0-alpha-1")); // alphas do not matter
            softly.assertThat(current).isEqualTo(new RoboZonkyVersion("4.1.0-beta-1")); // betas do not matter
            softly.assertThat(current).isEqualTo(new RoboZonkyVersion("4.1.0-cr-1")); // CRs do not matter
            softly.assertThat(current).isNotEqualTo(null);
            softly.assertThat(current).isNotEqualTo("something");
            softly.assertThat(current).isNotEqualTo(new RoboZonkyVersion("4.1.1"));
        });
    }
}
