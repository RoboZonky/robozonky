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

package com.github.robozonky.app.management;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class PortfolioTest {

    @Test
    public void run() {
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(1000);
        Mockito.when(portfolio.getCzkInvested()).thenReturn(10000);
        Mockito.when(portfolio.getShareOnInvestment(ArgumentMatchers.any())).thenReturn(BigDecimal.ONE);
        Mockito.when(portfolio.getCzkInvested(ArgumentMatchers.any())).thenReturn(1000);
        final Portfolio mbean = new Portfolio();
        final ExecutionStartedEvent evt = new ExecutionStartedEvent(Collections.emptyList(), portfolio);
        mbean.handle(evt);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mbean.getAvailableBalance()).isEqualTo(portfolio.getCzkAvailable());
            softly.assertThat(mbean.getInvestedAmount()).isEqualTo(portfolio.getCzkInvested());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isBeforeOrEqualTo(OffsetDateTime.now());
            // checks for proper ordering of ratings
            final String[] ratings = Stream.of(Rating.values()).map(Rating::getCode).toArray(String[]::new);
            softly.assertThat(mbean.getInvestedAmountPerRating().keySet()).containsExactly(ratings);
            softly.assertThat(mbean.getRatingShare().keySet()).containsExactly(ratings);
            //checks correct values per rating
            Stream.of(ratings).forEach(r -> {
                softly.assertThat(mbean.getInvestedAmountPerRating()).containsEntry(r, 1000);
                softly.assertThat(mbean.getRatingShare()).containsEntry(r, BigDecimal.ONE);
            });
        });
    }
}
