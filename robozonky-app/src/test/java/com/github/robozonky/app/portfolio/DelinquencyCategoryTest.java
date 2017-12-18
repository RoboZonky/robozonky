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

package com.github.robozonky.app.portfolio;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class DelinquencyCategoryTest extends AbstractZonkyLeveragingTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Stream.of(DelinquencyCategory.values()).map(c -> new Object[]{c}).collect(Collectors.toSet());
    }

    @Parameterized.Parameter
    public DelinquencyCategory category;
    private TemporalAmount minimumMatchingDuration;

    @Before
    public void prepareTimelines() {
        minimumMatchingDuration = Period.ofDays(category.getThresholdInDays());
    }

    @Test
    public void empty() {
        Assertions.assertThat(category.update(Collections.emptyList(), null,
                                              new ZonkyLoanProvider())).isEmpty();
    }

    @Test
    public void addAndRead() {
        final int loanId = 1;
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(loanId))).thenReturn(new Loan(loanId, 200));
        // store a delinquent loan
        final Delinquent d = new Delinquent(loanId);
        final Delinquency dy = d.addDelinquency(LocalDate.now().minus(minimumMatchingDuration));
        Assertions.assertThat(category.update(Collections.singleton(dy), mockAuthentication(zonky),
                                              new ZonkyLoanProvider()))
                .containsExactly(loanId);
        final List<Event> events = this.getNewEvents();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(events).hasSize(1);
            softly.assertThat(events).first().isInstanceOf(LoanDelinquentEvent.class);
        });
        // attempt to store it again, making sure no event is fired
        Assertions.assertThat(category.update(Collections.singleton(dy), mockAuthentication(zonky),
                                              new ZonkyLoanProvider()))
                .containsExactly(loanId);
        Assertions.assertThat(this.getNewEvents()).isEqualTo(events);
        // now update with no delinquents, making sure nothing is returned
        Assertions.assertThat(category.update(Collections.emptyList(), mockAuthentication(zonky),
                                              new ZonkyLoanProvider())).isEmpty();
        Assertions.assertThat(this.getNewEvents()).isEqualTo(events);
    }
}
