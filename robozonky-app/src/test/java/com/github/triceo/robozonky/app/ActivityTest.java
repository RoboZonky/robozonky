/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import com.github.triceo.robozonky.api.remote.Api;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.app.configuration.Configuration;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class ActivityTest extends AbstractStateLeveragingTest {

    private static final int CHECK_TIMEOUT_MINUTES = 60;

    private final Configuration ctx = new Configuration(1, 200, ActivityTest.CHECK_TIMEOUT_MINUTES, 120);

    @Test
    public void timestampFailover() {
        Activity.STATE.setValue(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, "definitelyNotADate");
        Assertions.assertThat(Activity.getLatestMarketplaceAction()).isNotNull();
    }

    @Test
    public void doesNotSleepWhenFirstRunButSleepsOnSecondAttempt() {
        final Activity activity = new Activity(ctx, Mockito.mock(Api.class));
        Assertions.assertThat(activity.shouldSleep()).isFalse();
        activity.settle();
        // this will show that the marketplace check works
        Assertions.assertThat(activity.shouldSleep()).isTrue();
    }

    @Test
    public void doesWakeUpWhenNewLoanAndThenSleeps() throws IOException {
        // make sure we have a marketplace check timestamp that would fall into sleeping range
        final OffsetDateTime timestamp =
                OffsetDateTime.now().minus(ActivityTest.CHECK_TIMEOUT_MINUTES / 2, ChronoUnit.MINUTES);
        Activity.STATE.setValue(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, timestamp.toString());
        // load API that has loans more recent than that, but makes sure not to come within the closed period
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getDatePublished()).thenReturn(timestamp.plus(10, ChronoUnit.MINUTES));
        Mockito.when(l.getRemainingInvestment()).thenReturn(1000.0);
        final Api zonkyApi = Mockito.mock(Api.class);
        Mockito.when(zonkyApi.getLoans()).thenReturn(Collections.singletonList(l));
        // test proper wakeup
        final Activity activity = new Activity(ctx, zonkyApi);
        Assertions.assertThat(activity.shouldSleep()).isFalse();
        activity.settle();
        // after which it should properly fall asleep again
        Assertions.assertThat(activity.shouldSleep()).isTrue();
        // and make sure that the timestamp has changed to a new reasonable value
        final OffsetDateTime newTimestamp =
                OffsetDateTime.parse(Activity.STATE.getValue(Activity.LAST_MARKETPLACE_CHECK_STATE_ID).get());
        Assertions.assertThat(newTimestamp.isAfter(l.getDatePublished()));
    }

    @Test
    public void doesTakeIntoAccountClosedPeriod() throws IOException {
        // make sure we have a marketplace check timestamp that would fall into sleeping range
        final OffsetDateTime timestamp = OffsetDateTime.now();
        Activity.STATE.setValue(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, timestamp.toString());
        // load API that has loans within the closed period
        final Loan activeLoan = Mockito.mock(Loan.class);
        Mockito.when(activeLoan.getDatePublished()).thenReturn(timestamp.minus(1, ChronoUnit.SECONDS));
        Mockito.when(activeLoan.getRemainingInvestment()).thenReturn(1000.0);
        final Loan ignoredLoan = Mockito.mock(Loan.class);
        Mockito.when(ignoredLoan.getDatePublished()).thenReturn(timestamp.plus(1, ChronoUnit.SECONDS));
        Mockito.when(ignoredLoan.getRemainingInvestment()).thenReturn(100.0); // not enough => ignored
        final Api zonkyApi = Mockito.mock(Api.class);
        Mockito.when(zonkyApi.getLoans()).thenReturn(Arrays.asList(activeLoan, ignoredLoan));
        // there is nothing to do, so the app should fall asleep...
        final Activity activity = new Activity(ctx, zonkyApi);
        Assertions.assertThat(activity.shouldSleep()).isTrue();
        activity.settle();
        // ... but reconfigure the timestamp so that we treat the closed-season loans as new loans
        final OffsetDateTime newTimestamp =
                OffsetDateTime.parse(Activity.STATE.getValue(Activity.LAST_MARKETPLACE_CHECK_STATE_ID).get());
        Assertions.assertThat(newTimestamp).isBefore(activeLoan.getDatePublished());
    }

}
