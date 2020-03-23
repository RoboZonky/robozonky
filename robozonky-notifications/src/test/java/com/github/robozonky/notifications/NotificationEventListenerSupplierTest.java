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

package com.github.robozonky.notifications;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.notifications.listeners.RoboZonkyInitializedEventListener;

class NotificationEventListenerSupplierTest {

    private static ConfigStorage mockProperties() throws IOException {
        final InputStream is = RoboZonkyInitializedEventListener.class
            .getResourceAsStream("notifications-enabled-spamless.cfg");
        return mockProperties(is);
    }

    private static ConfigStorage mockProperties(final InputStream is) throws IOException {
        return spy(ConfigStorage.create(is));
    }

    @Test
    void lifecycle() throws IOException {
        final NotificationEventListenerSupplier<RoboZonkyInitializedEvent> s = new NotificationEventListenerSupplier<>(
                RoboZonkyInitializedEvent.class);
        assertThat(s.apply(Target.EMAIL)).isNull();
        // the listener is enabled here
        final ConfigStorage p = mockProperties(
                RoboZonkyInitializedEventListener.class.getResourceAsStream("notifications-enabled.cfg"));
        s.configure(p);
        assertThat(s.apply(Target.EMAIL)).isNotNull();
        // disabled here
        final ConfigStorage p2 = mockProperties();
        s.configure(p2);
        assertThat(s.apply(Target.EMAIL)).isNull();
        // and re-enabled
        s.configure(p);
        assertThat(s.apply(Target.EMAIL)).isNotNull();
        s.disable();
        assertThat(s.apply(Target.EMAIL)).isNull();
    }

    @Test
    void setDisabled() throws IOException {
        final NotificationEventListenerSupplier<RoboZonkyCrashedEvent> s = new NotificationEventListenerSupplier<>(
                RoboZonkyCrashedEvent.class);
        final ConfigStorage p = mockProperties();
        s.configure(p);
        assertThat(s.apply(Target.EMAIL)).isNull();
    }
}
