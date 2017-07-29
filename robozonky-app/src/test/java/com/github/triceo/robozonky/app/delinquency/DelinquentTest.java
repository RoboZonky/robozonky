/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.delinquency;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class DelinquentTest {

    @Test
    public void loanRetrieval() {
        final int loanId = 1;
        final Delinquent d = new Delinquent(loanId);
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(loanId))).thenReturn(new Loan(loanId, 200));
        final Loan l = d.getLoan(zonky);
        Mockito.verify(zonky).getLoan(ArgumentMatchers.eq(loanId));
        Assertions.assertThat(l).isNotNull();
        Assertions.assertThat(l.getId()).isEqualTo(loanId);
    }

    @Test
    public void withActiveDelinquency() {
        final OffsetDateTime since = OffsetDateTime.now();
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
            softly.assertThat(active.getDetectedOn()).isEqualTo(since);
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
        final OffsetDateTime since = LocalDate.now().atStartOfDay(Defaults.ZONE_ID).toOffsetDateTime();
        // add active delinquency
        final Delinquency d1 = d.addDelinquency(since);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d1.getParent()).isEqualTo(d);
            softly.assertThat(d1.getDetectedOn()).isEqualTo(since);
            softly.assertThat(d1.getFixedOn()).isEmpty();
            softly.assertThat(d.getActiveDelinquency()).contains(d1);
        });
        // replace that delinquency with an inactive one
        final OffsetDateTime fixed = OffsetDateTime.now();
        final Delinquency d2 = d.addDelinquency(since, fixed);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d2).isSameAs(d1);
            softly.assertThat(d2.getFixedOn()).contains(fixed);
            softly.assertThat(d.getActiveDelinquency()).isEmpty();
        });
        // add another active dependency
        final OffsetDateTime next = OffsetDateTime.now();
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
