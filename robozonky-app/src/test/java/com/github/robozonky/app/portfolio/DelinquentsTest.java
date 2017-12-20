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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Random;

import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class DelinquentsTest extends AbstractZonkyLeveragingTest {

    private final static Random RANDOM = new Random(0);

    @Test
    public void empty() {
        Assertions.assertThat(Delinquents.getDelinquents()).isEmpty();
        Assertions.assertThat(this.getNewEvents()).isEmpty();
    }

    @Test
    public void newDelinquence() {
        final Loan l = new Loan(RANDOM.nextInt(10000), 200);
        final Investment i = Mockito.spy(new Investment(l, 200));
        Mockito.doReturn(OffsetDateTime.now().minusDays(1)).when(i).getNextPaymentDate();
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(l.getId()))).thenReturn(l);
        // make sure new delinquences are reported and stored
        Delinquents.update(mockAuthentication(zonky), Collections.singleton(i), Collections.emptyList(),
                           new ZonkyLoanProvider());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(Delinquents.getDelinquents()).hasSize(1);
            softly.assertThat(this.getNewEvents()).hasSize(1);
        });
        Assertions.assertThat(this.getNewEvents().get(0)).isInstanceOf(LoanNowDelinquentEvent.class);
        // make sure delinquences are persisted even when there are none present
        Delinquents.update(mockAuthentication(zonky), Collections.emptyList(), Collections.emptyList(),
                           new ZonkyLoanProvider());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(Delinquents.getDelinquents()).hasSize(1);
            softly.assertThat(this.getNewEvents()).hasSize(2);
        });
        Assertions.assertThat(this.getNewEvents().get(1)).isInstanceOf(LoanNoLongerDelinquentEvent.class);
        // and when they are no longer active, they're gone for good
        Delinquents.update(mockAuthentication(zonky), Collections.emptyList(), Collections.singleton(i),
                           new ZonkyLoanProvider());
        Assertions.assertThat(Delinquents.getDelinquents()).hasSize(0);
    }

    @Test
    public void oldDelinquence() {
        final Loan l = new Loan(RANDOM.nextInt(10000), 200);
        final Investment i = Mockito.spy(new Investment(l, 200));
        Mockito.doReturn(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID)).when(i).getNextPaymentDate();
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(l.getId()))).thenReturn(l);
        // make sure new delinquences are reported and stored
        Delinquents.update(mockAuthentication(zonky), Collections.singleton(i), Collections.emptyList(),
                           new ZonkyLoanProvider());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(Delinquents.getDelinquents()).hasSize(1);
            softly.assertThat(this.getNewEvents()).hasSize(5);
        });
        Assertions.assertThat(this.getNewEvents().get(0)).isInstanceOf(LoanNowDelinquentEvent.class);
        Assertions.assertThat(this.getNewEvents().get(1)).isInstanceOf(LoanDelinquent10DaysOrMoreEvent.class);
        Assertions.assertThat(this.getNewEvents().get(2)).isInstanceOf(LoanDelinquent30DaysOrMoreEvent.class);
        Assertions.assertThat(this.getNewEvents().get(3)).isInstanceOf(LoanDelinquent60DaysOrMoreEvent.class);
        Assertions.assertThat(this.getNewEvents().get(4)).isInstanceOf(LoanDelinquent90DaysOrMoreEvent.class);
    }

    @Test
    public void noLongerDelinquent() {
        final Loan l = new Loan(RANDOM.nextInt(10000), 200);
        final Investment i = Mockito.spy(new Investment(l, 200));
        Mockito.doReturn(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID)).when(i).getNextPaymentDate();
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(l.getId()))).thenReturn(l);
        // register delinquence
        Delinquents.update(mockAuthentication(zonky), Collections.singleton(i), Collections.emptyList(),
                           new ZonkyLoanProvider());
        this.readPreexistingEvents(); // ignore events just emitted
        // the investment is no longer delinquent
        Delinquents.update(mockAuthentication(zonky), Collections.emptyList(), Collections.emptyList(),
                           new ZonkyLoanProvider());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(this.getNewEvents()).hasSize(1).first().isInstanceOf(LoanNoLongerDelinquentEvent.class);
        });
    }

    @Test
    public void defaulted() {
        final Loan l = new Loan(RANDOM.nextInt(10000), 200);
        final Investment i = Mockito.spy(new Investment(l, 200));
        Mockito.doReturn(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID)).when(i).getNextPaymentDate();
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(l.getId()))).thenReturn(l);
        // register delinquence
        Delinquents.update(mockAuthentication(zonky), Collections.singleton(i), Collections.emptyList(),
                           new ZonkyLoanProvider());
        this.readPreexistingEvents(); // ignore events just emitted
        // the investment is defaulted
        Delinquents.update(mockAuthentication(zonky), Collections.emptyList(), Collections.singletonList(i),
                           new ZonkyLoanProvider());
        Assertions.assertThat(this.getNewEvents()).hasSize(1).first().isInstanceOf(LoanDefaultedEvent.class);
    }

    @Before
    @After
    public void clean() {
        Delinquents.update(mockAuthentication(Mockito.mock(Zonky.class)), Collections.emptyList(),
                           Collections.emptyList(),
                           new ZonkyLoanProvider());
    }
}
