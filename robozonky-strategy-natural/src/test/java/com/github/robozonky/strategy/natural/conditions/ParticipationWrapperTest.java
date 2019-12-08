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

package com.github.robozonky.strategy.natural.conditions;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Predicate;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.strategy.natural.Wrapper;
import com.github.robozonky.test.mock.MockLoanBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class ParticipationWrapperTest {

    private static final PortfolioOverview FOLIO = mock(PortfolioOverview.class);

    private static final Loan LOAN = new MockLoanBuilder()
            .setInsuranceActive(true)
            .setAmount(100_000)
            .setRating(Rating.D)
            .setInterestRate(Ratio.ONE)
            .setMainIncomeType(MainIncomeType.EMPLOYMENT)
            .setPurpose(Purpose.AUTO_MOTO)
            .setRegion(Region.JIHOCESKY)
            .setStory(UUID.randomUUID().toString())
            .setTermInMonths(20)
            .build();
    private static final Participation PARTICIPATION = mockParticipation(LOAN);

    private static Participation mockParticipation(final Loan loan) {
        final Participation p = mock(Participation.class);
        when(p.getInterestRate()).thenReturn(Ratio.ONE);
        doReturn(Money.ZERO).when(p).getRemainingPrincipal();
        doReturn(loan.getPurpose()).when(p).getPurpose();
        doReturn(loan.getRating()).when(p).getRating();
        doReturn(loan.getMainIncomeType()).when(p).getIncomeType();
        return p;
    }

    @Test
    void values() {
        final ParticipationDescriptor original = new ParticipationDescriptor(PARTICIPATION, () -> LOAN);
        final Wrapper<ParticipationDescriptor> w = Wrapper.wrap(original, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.isInsuranceActive()).isEqualTo(PARTICIPATION.isInsuranceActive());
            softly.assertThat(w.getInterestRate()).isEqualTo(Ratio.ONE);
            softly.assertThat(w.getRegion()).isEqualTo(LOAN.getRegion());
            softly.assertThat(w.getRating()).isEqualTo(PARTICIPATION.getRating());
            softly.assertThat(w.getMainIncomeType()).isEqualTo(LOAN.getMainIncomeType());
            softly.assertThat(w.getPurpose()).isEqualTo(LOAN.getPurpose());
            softly.assertThat(w.getOriginalAmount()).isEqualTo(LOAN.getAmount().getValue().intValue());
            softly.assertThat(w.getRemainingPrincipal()).isEqualTo(PARTICIPATION.getRemainingPrincipal().getValue());
            softly.assertThat(w.getOriginal()).isSameAs(original);
            softly.assertThat(w.getStory()).isEqualTo(LOAN.getStory());
            softly.assertThat(w.getOriginalTermInMonths()).isEqualTo(PARTICIPATION.getOriginalInstalmentCount());
            softly.assertThat(w.getRemainingTermInMonths()).isEqualTo(PARTICIPATION.getRemainingInstalmentCount());
            softly.assertThat(w.toString()).isNotNull();
        });
    }

    @Test
    void equality() {
        final ParticipationDescriptor original = new ParticipationDescriptor(PARTICIPATION, () -> LOAN);
        final Wrapper<ParticipationDescriptor> w = Wrapper.wrap(original, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w).isEqualTo(w);
            softly.assertThat(w).isEqualTo(Wrapper.wrap(original, FOLIO));
            softly.assertThat(w).isEqualTo(Wrapper.wrap(new ParticipationDescriptor(PARTICIPATION, () -> LOAN), FOLIO));
            softly.assertThat(w).isNotEqualTo(null);
        });
    }

    @Test
    void elseConditionWorks() {
        final AbstractEnumeratedCondition<Region> c = new BorrowerRegionCondition();
        c.add(Region.USTECKY);
        c.add(Region.MORAVSKOSLEZSKY);
        c.add(Region.KARLOVARSKY);
        final MarketplaceFilter f = new MarketplaceFilter();
        f.when(Collections.singleton(c));
        final AbstractEnumeratedCondition<MainIncomeType> c2 = new BorrowerIncomeCondition();
        c2.add(MainIncomeType.EMPLOYMENT);
        final AbstractRangeCondition c3 = LoanInterestRateCondition.exact(Ratio.ZERO, Ratio.fromPercentage(16));
        f.butNotWhen(Arrays.asList(c2, c3));
        final Loan l = new MockLoanBuilder()
                .setRegion(Region.USTECKY)
                .build();
        final Participation p = mock(Participation.class);
        when(p.getIncomeType()).thenReturn(MainIncomeType.EMPLOYMENT);
        when(p.getInterestRate()).thenReturn(Ratio.fromPercentage(15));
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> l);
        final Wrapper<ParticipationDescriptor> w = Wrapper.wrap(pd, FOLIO);
        assertThat((Predicate<Wrapper<?>>) f).rejects(w);
    }
}
