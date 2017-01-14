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

package com.github.triceo.robozonky.notifications.email;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

public class EmailListenerServiceTest extends AbstractListenerTest {

    @Rule
    public final ProvideSystemProperty myPropertyHasMyValue = new ProvideSystemProperty(
            NotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
            NotificationPropertiesTest.class.getResource("notifications-enabled.cfg").toString());

    private final EmailListenerService service = new EmailListenerService();

    private <T extends Event> Refreshable<EventListener<T>> getListener(final Class<T> eventType) {
        final Refreshable<EventListener<T>> refreshable = service.findListener(eventType);
        refreshable.getDependedOn().ifPresent(Refreshable::run);
        refreshable.run();
        return refreshable;
    }

    @Test
    public void noPropertiesNoListeners() {
        System.setProperty(NotificationProperties.CONFIG_FILE_LOCATION_PROPERTY, "");
        Assertions.assertThat(getListener(this.event.getClass()).getLatest()).isEmpty();
    }

    @Test
    public void reportingEnabledHaveListeners() {
        Assertions.assertThat(getListener(this.event.getClass()).getLatest()).isPresent();
    }

}
