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

package com.github.robozonky.strategy.natural;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.test.mock.MockLoanBuilder;

class PrimaryMarketplaceComparatorTest {

    private final Comparator<LoanDescriptor> c = new PrimaryMarketplaceComparator(Rating::compareTo);

    private static LoanImpl mockLoan(final Rating rating, final int amount, final OffsetDateTime published) {
        return new MockLoanBuilder()
            .set(LoanImpl::setRating, rating)
            .set(LoanImpl::setDatePublished, published)
            .set(LoanImpl::setRemainingInvestment, Money.from(amount))
            .set(LoanImpl::setReservedAmount, Money.from(0))
            .build();
    }

    @Test
    void sortByRating() {
        final OffsetDateTime first = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONKYCZ_ZONE_ID);
        final OffsetDateTime second = first.plus(Duration.ofMillis(1));
        final LoanImpl l1 = mockLoan(Rating.D, 100000, first);
        final LoanImpl l2 = mockLoan(Rating.A, l1.getNonReservedRemainingInvestment()
            .getValue()
            .intValue(), second);
        final LoanDescriptor ld1 = new LoanDescriptor(l1), ld2 = new LoanDescriptor(l2);
        assertSoftly(softly -> {
            softly.assertThat(c.compare(ld1, ld2))
                .isGreaterThan(0);
            softly.assertThat(c.compare(ld2, ld1))
                .isLessThan(0);
            softly.assertThat(c.compare(ld1, ld1))
                .isEqualTo(0);
        });
    }

    @Test
    void sortByRecencyIfSameRating() {
        final OffsetDateTime first = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONKYCZ_ZONE_ID);
        final OffsetDateTime second = first.plus(Duration.ofMillis(1));
        final LoanImpl l1 = mockLoan(Rating.A, 100000, first);
        final LoanImpl l2 = mockLoan(Rating.A, l1.getNonReservedRemainingInvestment()
            .getValue()
            .intValue(), second);
        final LoanDescriptor ld1 = new LoanDescriptor(l1), ld2 = new LoanDescriptor(l2);
        assertSoftly(softly -> {
            softly.assertThat(c.compare(ld1, ld2))
                .isGreaterThan(0);
            softly.assertThat(c.compare(ld2, ld1))
                .isLessThan(0);
            softly.assertThat(c.compare(ld1, ld1))
                .isEqualTo(0);
        });
    }

    @Test
    void sortByRemainingIfAsRecent() {
        final OffsetDateTime first = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONKYCZ_ZONE_ID);
        final LoanImpl l1 = mockLoan(Rating.A, 100000, first);
        final LoanImpl l2 = mockLoan(Rating.A, l1.getNonReservedRemainingInvestment()
            .getValue()
            .intValue() + 1, l1.getDatePublished());
        final LoanDescriptor ld1 = new LoanDescriptor(l1), ld2 = new LoanDescriptor(l2);
        assertSoftly(softly -> {
            softly.assertThat(c.compare(ld1, ld2))
                .isGreaterThan(0);
            softly.assertThat(c.compare(ld2, ld1))
                .isLessThan(0);
        });
    }
}
