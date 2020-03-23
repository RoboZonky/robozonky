/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.cli;

import static org.assertj.core.api.Assertions.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.internal.extensions.ListenerServiceLoader;
import com.github.robozonky.internal.state.TenantState;

@ExtendWith(MockitoExtension.class)
class NotificationTestingFeatureTest {

    private static final SessionInfo SESSION_INFO = new SessionInfo(UUID.randomUUID()
        .toString());
    @Mock
    private EventListener<RoboZonkyTestingEvent> l;

    @Test
    void noNotifications() throws MalformedURLException, SetupFailedException {
        final String username = UUID.randomUUID()
            .toString();
        final URL url = new URL("http://localhost");
        final Feature f = new NotificationTestingFeature(username, url);
        f.setup();
        assertThatThrownBy(f::test).isInstanceOf(TestFailedException.class);
    }

    @Test
    void notificationsEmptyOnInput() {
        assertThat(NotificationTestingFeature.notifications(SESSION_INFO, Collections.emptyList())).isFalse();
    }

    @Test
    void notificationsEmptyByDefault() throws MalformedURLException {
        assertThat(NotificationTestingFeature.notifications(SESSION_INFO, new URL("file:///something"))).isFalse();
        assertThat(ListenerServiceLoader.getNotificationConfiguration(SESSION_INFO)).isNotEmpty();
    }

    @BeforeEach
    @AfterEach
    void destroyState() {
        TenantState.destroyAll();
    }

    @Test
    void notificationsProper() {
        final EventListenerSupplier<RoboZonkyTestingEvent> r = () -> Optional.of(l);
        assertThat(NotificationTestingFeature.notifications(SESSION_INFO, Collections.singletonList(r))).isTrue();
        assertThat(ListenerServiceLoader.getNotificationConfiguration(SESSION_INFO)).isEmpty();
        Mockito.verify(l)
            .handle(ArgumentMatchers.any(RoboZonkyTestingEvent.class), ArgumentMatchers.any());
    }
}
