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

package com.github.triceo.robozonky.app.notifications;

import java.util.Arrays;
import java.util.List;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.ListenerService;
import com.github.triceo.robozonky.api.notifications.RoboZonkyStartingEvent;
import com.github.triceo.robozonky.app.util.Scheduler;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class ListenerServiceLoaderTest {

    @Test
    public void correctLoading() {
        final RoboZonkyStartingEvent e = new RoboZonkyStartingEvent("");
        final EventListener<RoboZonkyStartingEvent> l = Mockito.mock(EventListener.class);
        final ListenerService s1 = Mockito.mock(ListenerService.class);
        final Refreshable<EventListener<RoboZonkyStartingEvent>> returned = Refreshable.createImmutable(l);
        returned.run();
        Mockito.doReturn(returned).when(s1).findListener(ArgumentMatchers.eq(e.getClass()));
        final ListenerService s2 = Mockito.mock(ListenerService.class);
        Mockito.doReturn(Refreshable.createImmutable()).when(s2).findListener(ArgumentMatchers.eq(e.getClass()));
        final Iterable<ListenerService> s = () -> Arrays.asList(s1, s2).iterator();
        final List<Refreshable<EventListener<RoboZonkyStartingEvent>>> r =
                ListenerServiceLoader.load(RoboZonkyStartingEvent.class, s, new Scheduler());
        Assertions.assertThat(r).hasSize(2);
        Assertions.assertThat(r)
                .first()
                .has(new Condition<>(result -> result.getLatest().isPresent() && result.getLatest().get() == l,
                        "Exists"));
        Assertions.assertThat(r)
                .last()
                .has(new Condition<>(result -> !result.getLatest().isPresent(), "Does not exist"));

    }

}
