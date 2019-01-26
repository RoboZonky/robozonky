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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class SecondaryMarketplaceComparatorTest {

    private final Comparator<ParticipationDescriptor> c = new SecondaryMarketplaceComparator();

    private static Loan mockLoan(final int id, final int amount) {
        return mockLoan(id, amount, false);
    }

    private static Loan mockLoan(final int id, final int amount, final boolean insured) {
        return Loan.custom()
                .setId(id)
                .setAmount(amount)
                .setNonReservedRemainingInvestment(amount)
                .setInsuranceActive(insured)
                .build();
    }

    private static ParticipationDescriptor mockParticipationDescriptor(final Loan loan) {
        return mockParticipationDescriptor(loan, 0);
    }

    private static ParticipationDescriptor mockParticipationDescriptor(final Loan loan,
                                                                       final int remainingInstalments) {
        return mockParticipationDescriptor(loan, remainingInstalments, OffsetDateTime.now());
    }

    private static ParticipationDescriptor mockParticipationDescriptor(final Loan loan,
                                                                       final int remainingInstalments,
                                                                       final OffsetDateTime deadline) {
        final Participation p = mock(Participation.class);
        when(p.getRemainingInstalmentCount()).thenReturn(remainingInstalments);
        when(p.getDeadline()).thenReturn(deadline);
        when(p.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(loan.getAmount()));
        return new ParticipationDescriptor(p, () -> loan);
    }

    @Test
    void sortByInsurance() {
        final Loan l1 = mockLoan(1, 100000, true);
        final Loan l2 = mockLoan(l1.getId() + 1, l1.getAmount(), !l1.isInsuranceActive());
        final ParticipationDescriptor pd1 = mockParticipationDescriptor(l1), pd2 = mockParticipationDescriptor(l2);
        assertSoftly(softly -> {
            softly.assertThat(c.compare(pd1, pd2)).isEqualTo(-1);
            softly.assertThat(c.compare(pd2, pd1)).isEqualTo(1);
            softly.assertThat(c.compare(pd1, pd1)).isEqualTo(0);
        });
    }

    @Test
    void sortByTermIfInsured() {
        final Loan l1 = mockLoan(1, 100000);
        final Loan l2 = mockLoan(l1.getId() + 1, l1.getAmount());
        final ParticipationDescriptor pd1 = mockParticipationDescriptor(l1, 2),
                pd2 = mockParticipationDescriptor(l2, 1);
        assertSoftly(softly -> {
            softly.assertThat(c.compare(pd1, pd2)).isEqualTo(1);
            softly.assertThat(c.compare(pd2, pd1)).isEqualTo(-1);
            softly.assertThat(c.compare(pd1, pd1)).isEqualTo(0);
        });
    }

}
