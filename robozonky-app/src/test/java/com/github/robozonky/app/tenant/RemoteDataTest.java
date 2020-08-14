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

package com.github.robozonky.app.tenant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.internal.util.functional.Tuple;
import com.github.robozonky.internal.util.functional.Tuple2;
import com.github.robozonky.test.mock.MockInvestmentBuilder;

class RemoteDataTest extends AbstractZonkyLeveragingTest {

    @Test
    void getters() {
        final Zonky zonky = harmlessZonky();
        final Tenant tenant = mockTenant(zonky);
        final RemoteData data = RemoteData.load(tenant);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(data.getStatistics())
                .isNotNull();
            softly.assertThat(data.getBlocked())
                .isEmpty();
            softly.assertThat(data.getRetrievedOn())
                .isBeforeOrEqualTo(OffsetDateTime.now());
        });
    }

    @Test
    void amountsBlocked() {
        final Zonky zonky = harmlessZonky();
        final Tenant tenant = mockTenant(zonky);
        Investment i = MockInvestmentBuilder.fresh()
            .set(InvestmentImpl::setRating, Rating.D)
            .set(InvestmentImpl::setAmount, Money.from(BigDecimal.TEN))
            .build();
        when(zonky.getInvestments(any())).thenReturn(Stream.of(i));
        Map<Integer, Tuple2<Rating, Money>> result = RemoteData.getAmountsBlocked(tenant);
        Assertions.assertThat(result)
            .containsOnly(Map.entry(i.getLoan()
                .getId(), Tuple.of(Rating.D, Money.from(10))));
    }

}
