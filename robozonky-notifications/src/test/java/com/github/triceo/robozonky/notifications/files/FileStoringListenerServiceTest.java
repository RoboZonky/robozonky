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

package com.github.triceo.robozonky.notifications.files;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyStartingEvent;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

public class FileStoringListenerServiceTest {

    @Rule
    public final ProvideSystemProperty myPropertyHasMyValue = new ProvideSystemProperty(
            RefreshableFileNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
            FileStoringListenerServiceTest.class.getResource("notifications-enabled.cfg").toString());

    private FileStoringListenerService service = new FileStoringListenerService();

    private <T extends Event> Refreshable<EventListener<T>> getListener(final Class<T> eventType) {
        final Refreshable<EventListener<T>> refreshable = service.findListener(eventType);
        refreshable.getDependedOn().get().run();
        refreshable.run();
        return refreshable;
    }

    @Test
    public void supports() {
        Assertions.assertThat(getListener(InvestmentDelegatedEvent.class).getLatest()).isPresent();
    }

    @Test
    public void supportsInvestmentRejectedEvent() {
        Assertions.assertThat(getListener(InvestmentRejectedEvent.class).getLatest()).isPresent();
    }

    @Test
    public void supportsInvestmentMadeEvent() {
        Assertions.assertThat(getListener(InvestmentMadeEvent.class).getLatest()).isPresent();
    }

    @Test
    public void supportsInvestmentSkippedEvent() {
        Assertions.assertThat(getListener(InvestmentSkippedEvent.class).getLatest()).isPresent();
    }

    @Test
    public void doesNotSupportsUnknownEvent() {
        Assertions.assertThat(getListener(RoboZonkyStartingEvent.class).getLatest()).isEmpty();
    }
}
