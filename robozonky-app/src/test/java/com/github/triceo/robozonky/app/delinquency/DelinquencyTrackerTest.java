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

package com.github.triceo.robozonky.app.delinquency;

import java.util.Collections;

import com.github.triceo.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.triceo.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.app.investing.AbstractInvestingTest;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class DelinquencyTrackerTest extends AbstractInvestingTest {

    @Test
    public void empty() {
        Assertions.assertThat(DelinquencyTracker.INSTANCE.getDelinquents()).isEmpty();
        DelinquencyTracker.INSTANCE.update(null, Collections.emptyList());
        Assertions.assertThat(DelinquencyTracker.INSTANCE.getDelinquents()).isEmpty();
        Assertions.assertThat(this.getNewEvents()).isEmpty();
    }

    @Test
    public void newDelinquence() {
        final Loan l = new Loan(1, 200);
        final Investment i = new Investment(l, 200);
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(l.getId()))).thenReturn(l);
        // make sure new delinquences are reported and stored
        DelinquencyTracker.INSTANCE.update(zonky, Collections.singleton(i));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(DelinquencyTracker.INSTANCE.getDelinquents()).hasSize(1);
            softly.assertThat(this.getNewEvents()).hasSize(1)
                    .first().isInstanceOf(LoanNowDelinquentEvent.class);
        });
        // make sure delinquences are persisted even when there are none present
        DelinquencyTracker.INSTANCE.update(zonky, Collections.emptyList());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(DelinquencyTracker.INSTANCE.getDelinquents()).hasSize(1);
            softly.assertThat(this.getNewEvents()).hasSize(2)
                    .last().isInstanceOf(LoanNoLongerDelinquentEvent.class);
        });
        // and when they are no longer active, they're gone for good
        DelinquencyTracker.INSTANCE.update(zonky, Collections.emptyList(), Collections.singleton(i));
        Assertions.assertThat(DelinquencyTracker.INSTANCE.getDelinquents()).hasSize(0);
    }
}
