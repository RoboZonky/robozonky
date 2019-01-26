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

package com.github.robozonky.common.extensions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.ListenerService;
import com.github.robozonky.api.notifications.RoboZonkyStartingEvent;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.common.state.TenantState;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListenerServiceLoaderTest {

    @Mock
    private EventListener<RoboZonkyStartingEvent> l;

    @AfterEach
    void deleteState() {
        TenantState.destroyAll();
    }

    @Test
    void correctLoading() {
        final ListenerService s1 = mock(ListenerService.class);
        final EventListenerSupplier<RoboZonkyStartingEvent> returned = () -> Optional.of(l);
        doAnswer(i -> Stream.of(returned)).when(s1).findListeners(eq(RoboZonkyStartingEvent.class));
        final ListenerService s2 = mock(ListenerService.class);
        doAnswer(i -> Stream.of((EventListenerSupplier<RoboZonkyStartingEvent>) Optional::empty))
                .when(s2).findListeners(eq(RoboZonkyStartingEvent.class));
        final Iterable<ListenerService> s = () -> Arrays.asList(s1, s2).iterator();
        final List<EventListenerSupplier<RoboZonkyStartingEvent>> r =
                ListenerServiceLoader.load(RoboZonkyStartingEvent.class, s);
        assertThat(r).hasSize(2);
        assertThat(r)
                .first()
                .has(new Condition<>(result -> result.get().isPresent() && Objects.equals(result.get().get(), l),
                                     "Exists"));
        assertThat(r)
                .last()
                .has(new Condition<>(result -> !result.get().isPresent(), "Does not exist"));
    }

    @Test
    void empty() {
        final List<EventListenerSupplier<RoboZonkyTestingEvent>> r =
                ListenerServiceLoader.load(RoboZonkyTestingEvent.class);
        assertThat(r).isEmpty(); // no providers registered by default
    }

    @Test
    void configuration() throws MalformedURLException {
        final SessionInfo sessionInfo = new SessionInfo("someone@somewhere.cz");
        final String url = "http://localhost";
        assertThat(ListenerServiceLoader.getNotificationConfiguration(sessionInfo)).isEmpty();
        ListenerServiceLoader.registerConfiguration(sessionInfo, new URL(url));
        assertThat(ListenerServiceLoader.getNotificationConfiguration(sessionInfo)).contains(url);
        ListenerServiceLoader.unregisterConfiguration(sessionInfo);
        assertThat(ListenerServiceLoader.getNotificationConfiguration(sessionInfo)).isEmpty();
    }
}
