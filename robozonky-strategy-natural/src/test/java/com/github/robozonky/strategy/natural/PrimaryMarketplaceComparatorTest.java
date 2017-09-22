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

package com.github.robozonky.strategy.natural;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.internal.api.Defaults;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class PrimaryMarketplaceComparatorTest {

    private final Comparator<LoanDescriptor> c = new PrimaryMarketplaceComparator();

    @Test
    public void sortByRecency() {
        final OffsetDateTime first = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        final OffsetDateTime second = first.plus(Duration.ofMillis(1));
        final Loan l1 = new Loan(1, 100000, first);
        final Loan l2 = new Loan(l1.getId() + 1, (int) l1.getAmount(), second);
        final LoanDescriptor ld1 = new LoanDescriptor(l1), ld2 = new LoanDescriptor(l2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(c.compare(ld1, ld2)).isEqualTo(1);
            softly.assertThat(c.compare(ld2, ld1)).isEqualTo(-1);
            softly.assertThat(c.compare(ld1, ld1)).isEqualTo(0);
        });
    }

    @Test
    public void sortByRemainingIfAsRecent() {
        final OffsetDateTime first = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        final Loan l1 = new Loan(1, 100000, first);
        final Loan l2 = new Loan(l1.getId() + 1, (int) l1.getAmount() + 1, l1.getDatePublished());
        final LoanDescriptor ld1 = new LoanDescriptor(l1), ld2 = new LoanDescriptor(l2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(c.compare(ld1, ld2)).isEqualTo(1);
            softly.assertThat(c.compare(ld2, ld1)).isEqualTo(-1);
        });
    }
}
