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

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class DelinquentTest {

    @Test
    public void withActiveDelinquency() {
        final LocalDate since = LocalDate.now();
        final int loanId = 1;
        final Delinquent d = new Delinquent(loanId, since);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d.getLoanId()).isEqualTo(loanId);
            softly.assertThat(d.getActiveDelinquency()).isPresent();
            softly.assertThat(d.hasActiveDelinquency()).isTrue();
            softly.assertThat(d.getDelinquencies()).hasSize(1);
        });
        final Delinquency active = d.getDelinquencies().findFirst().get();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(active.getParent()).isEqualTo(d);
            softly.assertThat(active.getPaymentMissedDate()).isEqualTo(since);
            softly.assertThat(active.getFixedOn()).isEmpty();
        });
    }

    @Test
    public void addDelinquency() {
        final Delinquent d = new Delinquent(1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d.getDelinquencies()).isEmpty();
            softly.assertThat(d.getActiveDelinquency()).isEmpty();
            softly.assertThat(d.hasActiveDelinquency()).isFalse();
        });
        final LocalDate since = LocalDate.now().minusDays(2);
        // add active delinquency
        final Delinquency d1 = d.addDelinquency(since);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d1.getParent()).isEqualTo(d);
            softly.assertThat(d1.getPaymentMissedDate()).isEqualTo(since);
            softly.assertThat(d1.getFixedOn()).isEmpty();
            softly.assertThat(d.getActiveDelinquency()).contains(d1);
        });
        // replace that delinquency with an inactive one
        final LocalDate fixed = LocalDate.now().minusDays(1);
        final Delinquency d2 = d.addDelinquency(since, fixed);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d2).isSameAs(d1);
            softly.assertThat(d2.getFixedOn()).contains(fixed);
            softly.assertThat(d.getActiveDelinquency()).isEmpty();
        });
        // add another active dependency
        final LocalDate next = LocalDate.now();
        final Delinquency d3 = d.addDelinquency(next);
        final Delinquency d4 = d.addDelinquency(next);
        Assertions.assertThat(d4).isSameAs(d3);
        Assertions.assertThat(d.getDelinquencies()).containsExactly(d1, d3);
    }

    @Test
    public void equality() {
        final Delinquent d = new Delinquent(1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d).isEqualTo(d);
            softly.assertThat(d).isNotEqualTo(null);
        });
        final Delinquent d2 = new Delinquent(d.getLoanId());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d).isEqualTo(d2);
            softly.assertThat(d2).isEqualTo(d);
        });
        final Delinquent d3 = new Delinquent(d.getLoanId() + 1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d).isNotEqualTo(d3);
            softly.assertThat(d3).isNotEqualTo(d);
        });
    }
}
