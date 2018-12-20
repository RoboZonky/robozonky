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

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class PrimaryMarketplaceComparatorTest {

    private final Comparator<LoanDescriptor> c = new PrimaryMarketplaceComparator();

    private static Loan mockLoan(final int id, final int amount, final OffsetDateTime published) {
        return mockLoan(id, amount, published, false);
    }

    private static Loan mockLoan(final int id, final int amount, final OffsetDateTime published,
                                 final boolean insured) {
        return Loan.custom()
                .setId(id)
                .setAmount(amount)
                .setDatePublished(published)
                .setNonReservedRemainingInvestment(amount)
                .setInsuranceActive(insured)
                .build();
    }

    @Test
    void sortByInsurance() {
        final OffsetDateTime first = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        final Loan l1 = mockLoan(1, 100000, first, true);
        final Loan l2 = mockLoan(l1.getId() + 1, l1.getAmount(), first, !l1.isInsuranceActive());
        final LoanDescriptor ld1 = new LoanDescriptor(l1), ld2 = new LoanDescriptor(l2);
        assertSoftly(softly -> {
            softly.assertThat(c.compare(ld1, ld2)).isEqualTo(-1);
            softly.assertThat(c.compare(ld2, ld1)).isEqualTo(1);
            softly.assertThat(c.compare(ld1, ld1)).isEqualTo(0);
        });
    }

    @Test
    void sortByRecencyIfInsured() {
        final OffsetDateTime first = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        final OffsetDateTime second = first.plus(Duration.ofMillis(1));
        final Loan l1 = mockLoan(1, 100000, first);
        final Loan l2 = mockLoan(l1.getId() + 1, l1.getAmount(), second);
        final LoanDescriptor ld1 = new LoanDescriptor(l1), ld2 = new LoanDescriptor(l2);
        assertSoftly(softly -> {
            softly.assertThat(c.compare(ld1, ld2)).isEqualTo(1);
            softly.assertThat(c.compare(ld2, ld1)).isEqualTo(-1);
            softly.assertThat(c.compare(ld1, ld1)).isEqualTo(0);
        });
    }

    @Test
    void sortByRemainingIfAsRecent() {
        final OffsetDateTime first = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        final Loan l1 = mockLoan(1, 100000, first);
        final Loan l2 = mockLoan(l1.getId() + 1, l1.getAmount() + 1, l1.getDatePublished());
        final LoanDescriptor ld1 = new LoanDescriptor(l1), ld2 = new LoanDescriptor(l2);
        assertSoftly(softly -> {
            softly.assertThat(c.compare(ld1, ld2)).isEqualTo(1);
            softly.assertThat(c.compare(ld2, ld1)).isEqualTo(-1);
        });
    }
}
