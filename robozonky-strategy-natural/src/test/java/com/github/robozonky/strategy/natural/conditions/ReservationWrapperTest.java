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

package com.github.robozonky.strategy.natural.conditions;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.internal.remote.entities.ReservationImpl;
import com.github.robozonky.strategy.natural.wrappers.Wrapper;
import com.github.robozonky.test.mock.MockReservationBuilder;

class ReservationWrapperTest {

    private static final PortfolioOverview FOLIO = mock(PortfolioOverview.class);

    @Test
    void values() {
        final Reservation l = new MockReservationBuilder()
            .set(ReservationImpl::setInsuranceActive, true)
            .set(ReservationImpl::setAmount, Money.from(100_000))
            .set(ReservationImpl::setInterestRate, Rating.D.getInterestRate())
            .set(ReservationImpl::setInterestRate, Ratio.ONE)
            .set(ReservationImpl::setMainIncomeType, MainIncomeType.EMPLOYMENT)
            .set(ReservationImpl::setPurpose, Purpose.AUTO_MOTO)
            .set(ReservationImpl::setRegion, Region.JIHOCESKY)
            .set(ReservationImpl::setStory, UUID.randomUUID()
                .toString())
            .set(ReservationImpl::setTermInMonths, 20)
            .build();
        final ReservationDescriptor original = new ReservationDescriptor(l, () -> null);
        final Wrapper<ReservationDescriptor> w = Wrapper.wrap(original, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.isInsuranceActive())
                .isEqualTo(l.isInsuranceActive());
            softly.assertThat(w.getInterestRate())
                .isEqualTo(l.getInterestRate());
            softly.assertThat(w.getRegion())
                .isEqualTo(l.getRegion());
            softly.assertThat(w.getMainIncomeType())
                .isEqualTo(l.getMainIncomeType());
            softly.assertThat(w.getPurpose())
                .isEqualTo(l.getPurpose());
            softly.assertThat(w.getOriginalAmount())
                .isEqualTo(l.getAmount()
                    .getValue()
                    .intValue());
            softly.assertThat(w.getRemainingPrincipal())
                .isEqualTo(BigDecimal.valueOf(w.getOriginalAmount()));
            softly.assertThat(w.getOriginal())
                .isSameAs(original);
            softly.assertThat(w.getStory())
                .isEqualTo(l.getStory());
            softly.assertThat(w.getOriginalTermInMonths())
                .isEqualTo(l.getTermInMonths());
            softly.assertThat(w.getRemainingTermInMonths())
                .isEqualTo(l.getTermInMonths());
            softly.assertThat(w.toString())
                .isNotNull();
        });
    }

    @Test
    void equality() {
        final Reservation l = new MockReservationBuilder()
            .set(ReservationImpl::setInsuranceActive, true)
            .set(ReservationImpl::setAmount, Money.from(100_000))
            .set(ReservationImpl::setInterestRate, Rating.D.getInterestRate())
            .set(ReservationImpl::setMainIncomeType, MainIncomeType.EMPLOYMENT)
            .set(ReservationImpl::setPurpose, Purpose.AUTO_MOTO)
            .set(ReservationImpl::setRegion, Region.JIHOCESKY)
            .set(ReservationImpl::setStory, UUID.randomUUID()
                .toString())
            .set(ReservationImpl::setTermInMonths, 20)
            .build();
        final ReservationDescriptor original = new ReservationDescriptor(l, () -> null);
        final Wrapper<ReservationDescriptor> w = Wrapper.wrap(original, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w)
                .isEqualTo(w);
            softly.assertThat(w)
                .isEqualTo(Wrapper.wrap(original, FOLIO));
            softly.assertThat(w)
                .isEqualTo(Wrapper.wrap(new ReservationDescriptor(l, () -> null), FOLIO));
            softly.assertThat(w)
                .isNotEqualTo(
                        Wrapper.wrap(new ReservationDescriptor(MockReservationBuilder.fresh(), () -> null), FOLIO));
            softly.assertThat(w)
                .isNotEqualTo(null);
        });
    }
}
