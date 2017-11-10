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

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.AbstractEventLeveragingRoboZonkyTest;
import com.github.robozonky.internal.api.Settings;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class ActivityTest extends AbstractEventLeveragingRoboZonkyTest {

    private static final int SLEEP_PERIOD_MINUTES = 60;

    @Test
    public void timestampFailover() {
        Activity.STATE.newBatch().set(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, "definitelyNotADate").call();
        Assertions.assertThat(Activity.getLatestMarketplaceAction()).isNotNull();
    }

    @Test
    public void doesWakeUpWhenNewLoanAndThenSleeps() throws IOException {
        // make sure we have a marketplace check timestamp that would fall into sleeping range
        final OffsetDateTime timestamp =
                OffsetDateTime.now().minus(ActivityTest.SLEEP_PERIOD_MINUTES / 2, ChronoUnit.MINUTES);
        Activity.STATE.newBatch().set(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, timestamp.toString()).call();
        // load API that has marketplace more recent than that, but makes sure not to come within the closed period
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getRating()).thenReturn(Rating.D);
        Mockito.when(l.getDatePublished()).thenReturn(timestamp.plus(10, ChronoUnit.MINUTES));
        Mockito.when(l.getRemainingInvestment()).thenReturn(1000.0);
        final LoanDescriptor ld = new LoanDescriptor(l);
        // test proper wakeup
        final Activity activity = new Activity(Collections.singletonList(ld));
        Assertions.assertThat(activity.shouldSleep()).isFalse();
        activity.settle();
        // after which it should properly fall asleep again
        Assertions.assertThat(activity.shouldSleep()).isTrue();
        // and make sure that the timestamp has changed to a new reasonable value
        final OffsetDateTime newTimestamp =
                OffsetDateTime.parse(Activity.STATE.getValue(Activity.LAST_MARKETPLACE_CHECK_STATE_ID).get());
        Assertions.assertThat(newTimestamp).isAfter(timestamp);
    }

    @Test
    public void doesTakeIntoAccountClosedPeriod() throws IOException {
        System.setProperty(Settings.Key.CAPTCHA_DELAY_AAAAA.getName(), "0"); // disable CAPTCHA for AAAAA rating
        System.setProperty(Settings.Key.CAPTCHA_DELAY_C.getName(), "120"); // enable CAPTCHA for C rating
        // make sure we have a marketplace check timestamp that would fall into sleeping range
        final OffsetDateTime timestamp = OffsetDateTime.now();
        Activity.STATE.newBatch().set(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, timestamp.toString()).call();
        // load API that has marketplace within the closed period
        final Loan activeLoan = Mockito.mock(Loan.class);
        Mockito.when(activeLoan.getId()).thenReturn(1);
        Mockito.when(activeLoan.getRating()).thenReturn(Rating.C); // captcha
        Mockito.when(activeLoan.getDatePublished()).thenReturn(timestamp.minus(1, ChronoUnit.SECONDS));
        Mockito.when(activeLoan.getRemainingInvestment()).thenReturn(1000.0);
        final Loan ignoredLoan = Mockito.mock(Loan.class);
        Mockito.when(ignoredLoan.getRating()).thenReturn(Rating.AAAAA); // no captcha
        Mockito.when(ignoredLoan.getId()).thenReturn(2);
        Mockito.when(ignoredLoan.getDatePublished()).thenReturn(timestamp);
        Mockito.when(ignoredLoan.getRemainingInvestment()).thenReturn(100.0); // not enough => ignored
        // there is nothing to do, so the app should fall asleep...
        final Activity activity =
                new Activity(Arrays.asList(new LoanDescriptor(activeLoan), new LoanDescriptor(ignoredLoan)));
        Assertions.assertThat(activity.shouldSleep()).isFalse();
        activity.settle();
        // ... but reconfigure the timestamp so that we treat the closed-season marketplace as new marketplace
        final OffsetDateTime newTimestamp =
                OffsetDateTime.parse(Activity.STATE.getValue(Activity.LAST_MARKETPLACE_CHECK_STATE_ID).get());
        Assertions.assertThat(newTimestamp).isBefore(activeLoan.getDatePublished());
    }
}
