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

package com.github.robozonky.app.investing;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.AbstractEventLeveragingTest;
import com.github.robozonky.internal.api.Settings;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityTest extends AbstractEventLeveragingTest {

    private static final int SLEEP_PERIOD_MINUTES = 60;

    @Test
    void timestampFailover() {
        Activity.STATE.newBatch().set(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, "definitelyNotADate").call();
        assertThat(Activity.getLatestMarketplaceAction()).isNotNull();
    }

    @Test
    void doesWakeUpWhenNewLoanAndThenSleeps() {
        // make sure we have a marketplace check timestamp that would fall into sleeping range
        final OffsetDateTime timestamp =
                OffsetDateTime.now().minus(ActivityTest.SLEEP_PERIOD_MINUTES / 2, ChronoUnit.MINUTES);
        Activity.STATE.newBatch().set(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, timestamp.toString()).call();
        // load API that has marketplace more recent than that, but makes sure not to come within the closed period
        final Loan l = mock(Loan.class);
        when(l.getRating()).thenReturn(Rating.D);
        when(l.getDatePublished()).thenReturn(timestamp.plus(10, ChronoUnit.MINUTES));
        when(l.getRemainingInvestment()).thenReturn(1000.0);
        final LoanDescriptor ld = new LoanDescriptor(l);
        // test proper wakeup
        final Activity activity = new Activity(Collections.singletonList(ld));
        assertThat(activity.shouldSleep()).isFalse();
        activity.settle();
        // after which it should properly fall asleep again
        assertThat(activity.shouldSleep()).isTrue();
        // and make sure that the timestamp has changed to a new reasonable value
        final OffsetDateTime newTimestamp =
                OffsetDateTime.parse(Activity.STATE.getValue(Activity.LAST_MARKETPLACE_CHECK_STATE_ID).get());
        assertThat(newTimestamp).isAfter(timestamp);
    }

    @Test
    void doesTakeIntoAccountClosedPeriod() {
        System.setProperty(Settings.Key.CAPTCHA_DELAY_AAAAA.getName(), "0"); // disable CAPTCHA for AAAAA rating
        System.setProperty(Settings.Key.CAPTCHA_DELAY_C.getName(), "120"); // enable CAPTCHA for C rating
        // make sure we have a marketplace check timestamp that would fall into sleeping range
        final OffsetDateTime timestamp = OffsetDateTime.now();
        Activity.STATE.newBatch().set(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, timestamp.toString()).call();
        // load API that has marketplace within the closed period
        final Loan activeLoan = mock(Loan.class);
        when(activeLoan.getId()).thenReturn(1);
        when(activeLoan.getRating()).thenReturn(Rating.C); // captcha
        when(activeLoan.getDatePublished()).thenReturn(timestamp.minus(1, ChronoUnit.SECONDS));
        when(activeLoan.getRemainingInvestment()).thenReturn(1000.0);
        final Loan ignoredLoan = mock(Loan.class);
        when(ignoredLoan.getRating()).thenReturn(Rating.AAAAA); // no captcha
        when(ignoredLoan.getId()).thenReturn(2);
        when(ignoredLoan.getDatePublished()).thenReturn(timestamp);
        when(ignoredLoan.getRemainingInvestment()).thenReturn(100.0); // not enough => ignored
        // there is nothing to do, so the app should fall asleep...
        final Activity activity =
                new Activity(Arrays.asList(new LoanDescriptor(activeLoan), new LoanDescriptor(ignoredLoan)));
        assertThat(activity.shouldSleep()).isFalse();
        activity.settle();
        // ... but reconfigure the timestamp so that we treat the closed-season marketplace as new marketplace
        final OffsetDateTime newTimestamp =
                OffsetDateTime.parse(Activity.STATE.getValue(Activity.LAST_MARKETPLACE_CHECK_STATE_ID).get());
        assertThat(newTimestamp).isBefore(activeLoan.getDatePublished());
    }
}
