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

package com.github.triceo.robozonky.app.purchasing;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.remote.entities.Participation;
import com.github.triceo.robozonky.api.strategies.ParticipationDescriptor;
import com.github.triceo.robozonky.api.strategies.PurchaseStrategy;
import com.github.triceo.robozonky.app.investing.AbstractInvestingTest;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class StrategyExecutionTest extends AbstractInvestingTest {

    @Test
    public void noStrategy() {
        final Participation mock = Mockito.mock(Participation.class);
        final ParticipationDescriptor pd = new ParticipationDescriptor(mock);
        final Refreshable<PurchaseStrategy> r = Refreshable.createImmutable(null);
        r.run();
        final StrategyExecution exec = new StrategyExecution(r, null, Duration.ofMinutes(60), true);
        Assertions.assertThat(exec.apply(Collections.singletonList(pd))).isEmpty();
        // check events
        final List<Event> events = this.getNewEvents();
        Assertions.assertThat(events).isEmpty();
    }
}
