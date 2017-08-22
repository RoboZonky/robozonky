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

package com.github.robozonky.notifications.email;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

public class EmailListenerServiceTest extends AbstractEmailingListenerTest {

    @Rule
    public final ProvideSystemProperty myPropertyHasMyValue = new ProvideSystemProperty(
            RefreshableNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
            NotificationPropertiesTest.class.getResource("notifications-enabled.cfg").toString());

    private final EmailListenerService service = new EmailListenerService();

    private <T extends Event> Refreshable<EventListener<T>> getListener(final Class<T> eventType) {
        final Refreshable<EventListener<T>> refreshable = service.findListener(eventType);
        refreshable.run();
        return refreshable;
    }

    @Test
    public void noPropertiesNoListeners() {
        System.setProperty(RefreshableNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY, "");
        Assertions.assertThat(getListener(this.event.getClass()).getLatest()).isEmpty();
    }

    @Test
    public void reportingEnabledHaveListeners() {
        Assertions.assertThat(getListener(this.event.getClass()).getLatest()).isPresent();
    }
}
