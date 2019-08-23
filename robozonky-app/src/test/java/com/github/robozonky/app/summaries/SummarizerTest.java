/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.app.summaries;

import com.github.robozonky.api.notifications.WeeklySummaryEvent;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SummarizerTest extends AbstractZonkyLeveragingTest {

    private final Zonky zonky = harmlessZonky();
    private final PowerTenant tenant = mockTenant(zonky);

    @Test
    void basics() {
        final Summarizer summarizer = new Summarizer();
        summarizer.accept(tenant);
        assertThat(this.getEventsRequested())
                .first()
                .isInstanceOf(WeeklySummaryEvent.class);
        final WeeklySummaryEvent evt = (WeeklySummaryEvent)this.getEventsRequested().get(0);
        final ExtendedPortfolioOverview summary = evt.getPortfolioOverview();
        assertThat(summary).isNotNull();
    }
}
