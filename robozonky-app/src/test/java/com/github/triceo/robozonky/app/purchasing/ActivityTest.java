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

package com.github.triceo.robozonky.app.purchasing;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Participation;
import com.github.triceo.robozonky.app.AbstractEventsAndStateLeveragingTest;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class ActivityTest extends AbstractEventsAndStateLeveragingTest {

    private static final int SLEEP_PERIOD_MINUTES = 60;

    @Test
    public void timestampFailover() {
        Activity.STATE.newBatch().set(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, "definitelyNotADate").call();
        Assertions.assertThat(Activity.getLatestMarketplaceAction())
                .isEqualTo(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
    }

    @Test
    public void noTimestamp() {
        Activity.STATE.newBatch(true).call();
        Assertions.assertThat(Activity.getLatestMarketplaceAction())
                .isEqualTo(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
    }

    @Test
    public void properTimestamp() {
        final OffsetDateTime now = OffsetDateTime.now();
        Activity.STATE.newBatch().set(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, now.toString()).call();
        Assertions.assertThat(Activity.getLatestMarketplaceAction()).isEqualTo(now);
    }

    @Test
    public void doesWakeUpWhenNewParticipationAndThenSleeps() throws IOException {
        // make sure we have a marketplace check timestamp that would fall into sleeping range
        final OffsetDateTime timestamp =
                OffsetDateTime.now().minus(ActivityTest.SLEEP_PERIOD_MINUTES / 2, ChronoUnit.MINUTES);
        Activity.STATE.newBatch()
                .set(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, timestamp.toString())
                .unset(Activity.LAST_MARKETPLACE_STATE_ID)
                .call();
        // load API that has marketplace more recent than that
        final Loan l = new Loan(1, 200);
        final Participation p = Mockito.mock(Participation.class);
        // test proper wakeup
        final Activity activity = new Activity(Collections.singletonList(p));
        Assertions.assertThat(activity.shouldSleep()).isFalse();
        activity.settle();
        SoftAssertions.assertSoftly(softly -> {
            // after which it should properly fall asleep again
            softly.assertThat(activity.shouldSleep()).isTrue();
            // marketplace status has been stored
            softly.assertThat(Activity.STATE.getValue(Activity.LAST_MARKETPLACE_STATE_ID)).isPresent();
            // timestamp has changed to a new reasonable value
            final OffsetDateTime newTimestamp =
                    OffsetDateTime.parse(Activity.STATE.getValue(Activity.LAST_MARKETPLACE_CHECK_STATE_ID).get());
            softly.assertThat(newTimestamp).isAfter(timestamp);
        });
    }
}


