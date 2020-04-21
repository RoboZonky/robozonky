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
import static org.mockito.Mockito.*;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.internal.remote.entities.ParticipationImpl;
import com.github.robozonky.test.mock.MockLoanBuilder;

class SecondaryMarketplaceComparatorTest {

    private final Comparator<ParticipationDescriptor> c = new SecondaryMarketplaceComparator(Rating::compareTo);

    private static ParticipationDescriptor mockParticipationDescriptor(final Loan loan) {
        final Participation p = mock(ParticipationImpl.class);
        Rating r = loan.getRating();
        when(p.getRating()).thenReturn(r);
        return new ParticipationDescriptor(p, () -> loan);
    }

    @Test
    void sortByRating() {
        final Loan l1 = new MockLoanBuilder()
            .setRating(Rating.D)
            .build();
        final Loan l2 = new MockLoanBuilder()
            .setRating(Rating.A)
            .build();
        final ParticipationDescriptor pd1 = mockParticipationDescriptor(l1),
                pd2 = mockParticipationDescriptor(l2);
        assertSoftly(softly -> {
            softly.assertThat(c.compare(pd1, pd2))
                .isGreaterThan(0);
            softly.assertThat(c.compare(pd2, pd1))
                .isLessThan(0);
            softly.assertThat(c.compare(pd1, pd1))
                .isEqualTo(0);
        });
    }

}
