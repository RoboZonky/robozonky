/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.notifications.email;

import java.util.Properties;

import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailingEventListenerSupplierTest {

    private static NotificationProperties mockProperties(final SupportedListener listener) {
        final NotificationProperties p = spy(new NotificationProperties(new Properties()));
        doReturn(false).when(p).isListenerEnabled(any());
        doReturn(true).when(p).isListenerEnabled(eq(listener));
        return p;
    }

    @Test
    void lifecycle() {
        final EmailingEventListenerSupplier<RoboZonkyTestingEvent> s =
                new EmailingEventListenerSupplier<>(RoboZonkyTestingEvent.class);
        assertThat(s.get()).isEmpty();
        final NotificationProperties p = mockProperties(SupportedListener.TESTING);
        s.valueSet(p);
        assertThat(s.get())
                .isPresent()
                .containsInstanceOf(RoboZonkyTestingEventListener.class);
        s.valueUnset(p);
        assertThat(s.get()).isEmpty();
    }

    @Test
    void setDisabled() {
        final EmailingEventListenerSupplier<RoboZonkyTestingEvent> s =
                new EmailingEventListenerSupplier<>(RoboZonkyTestingEvent.class);
        assertThat(s.get()).isEmpty();
        final NotificationProperties p = mockProperties(SupportedListener.TESTING);
        s.valueSet(p);
        assertThat(s.get())
                .isPresent()
                .containsInstanceOf(RoboZonkyTestingEventListener.class);
        // new properties don't have this listener enabled, therefore empty optional should be returned
        final NotificationProperties p2 = mockProperties(SupportedListener.CRASHED);
        s.valueChanged(p, p2);
        assertThat(s.get()).isEmpty();
    }
}
