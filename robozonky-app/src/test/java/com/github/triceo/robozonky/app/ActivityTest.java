/*
 * Copyright 2016 Lukáš Petrovický
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import com.github.triceo.robozonky.remote.Api;
import com.github.triceo.robozonky.remote.Loan;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ActivityTest {

    private static final int CHECK_TIMEOUT_MINUTES = 60;

    private final File activityCheckFile;
    private final AppContext ctx = new AppContext(1, 200, ActivityTest.CHECK_TIMEOUT_MINUTES, 120);

    public ActivityTest() {
        try {
            this.activityCheckFile = File.createTempFile("activity-", ".robozonky");
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Before
    public void deleteActivityFile() {
        this.activityCheckFile.delete();
    }

    @Test
    public void doesNotSleepWhenFirstRunButSleepsOnSecondAttempt() {
        final Activity activity = new Activity(ctx, Mockito.mock(Api.class), activityCheckFile.toPath());
        Assertions.assertThat(activity.shouldSleep()).isFalse();
        // this will show that the marketplace check works
        Assertions.assertThat(activity.shouldSleep()).isTrue();
    }

    @Test
    public void doesWakeUpWhenNewLoanAndThenSleeps() throws IOException {
        // make sure we have a marketplace check timestamp that would fall into sleeping range
        final Instant timestamp = Instant.now().minus(ActivityTest.CHECK_TIMEOUT_MINUTES / 2, ChronoUnit.MINUTES);
        Files.write(activityCheckFile.toPath(), Collections.singleton(timestamp.toString()));
        // load API that has loans more recent than that, but makes sure not to come within the closed period
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getDatePublished()).thenReturn(timestamp.plus(10, ChronoUnit.MINUTES));
        Mockito.when(l.getRemainingInvestment()).thenReturn(1000.0);
        final Api zonkyApi = Mockito.mock(Api.class);
        Mockito.when(zonkyApi.getLoans()).thenReturn(Collections.singletonList(l));
        // test proper wakeup
        final Activity activity = new Activity(ctx, zonkyApi, activityCheckFile.toPath());
        Assertions.assertThat(activity.shouldSleep()).isFalse();
        // after which it should properly fall asleep again
        Assertions.assertThat(activity.shouldSleep()).isTrue();
        // and make sure that the timestamp has changed to a new reasonable value
        final Instant newTimestamp = Instant.parse(Files.readAllLines(activityCheckFile.toPath()).get(0));
        Assertions.assertThat(newTimestamp.isAfter(l.getDatePublished()));
    }

    @Test
    public void doesTakeIntoAccountClosedPeriod() throws IOException {
        // make sure we have a marketplace check timestamp that would fall into sleeping range
        final Instant timestamp = Instant.now();
        Files.write(activityCheckFile.toPath(), Collections.singleton(timestamp.toString()));
        // load API that has loans within the closed period
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getDatePublished()).thenReturn(timestamp.minus(1, ChronoUnit.SECONDS));
        Mockito.when(l.getRemainingInvestment()).thenReturn(1000.0);
        final Api zonkyApi = Mockito.mock(Api.class);
        Mockito.when(zonkyApi.getLoans()).thenReturn(Collections.singletonList(l));
        // there is nothing to do, so the app should fall asleep...
        final Activity activity = new Activity(ctx, zonkyApi, activityCheckFile.toPath());
        Assertions.assertThat(activity.shouldSleep()).isTrue();
        // ... but reconfigure the timestamp so that we treat the closed-season loans as new loans
        final Instant newTimestamp = Instant.parse(Files.readAllLines(activityCheckFile.toPath()).get(0));
        Assertions.assertThat(newTimestamp).isLessThan(l.getDatePublished());
    }

}
