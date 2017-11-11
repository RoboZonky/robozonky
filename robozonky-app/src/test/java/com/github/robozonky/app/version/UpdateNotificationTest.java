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

package com.github.robozonky.app.version;

import java.util.Collection;
import java.util.Objects;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.RoboZonkyExperimentalUpdateDetectedEvent;
import com.github.robozonky.api.notifications.RoboZonkyUpdateDetectedEvent;
import com.github.robozonky.app.AbstractEventLeveragingRoboZonkyTest;
import com.github.robozonky.app.Events;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class UpdateNotificationTest extends AbstractEventLeveragingRoboZonkyTest {

    @Test
    public void valueChangeDelegates() {
        final UpdateNotification n = Mockito.spy(new UpdateNotification());
        final VersionIdentifier newValue = new VersionIdentifier("1.0.1");
        n.valueChanged(null, newValue);
        Mockito.verify(n).valueSet(ArgumentMatchers.eq(newValue));
    }

    @Test
    public void stableUpdate() {
        final String currentVersion = "1.0.0";
        final String newVersion = "1.0.1";
        final VersionIdentifier newVersionIdentifier = new VersionIdentifier(newVersion);
        final UpdateNotification n = new UpdateNotification(currentVersion);
        // check that the event is fired
        n.valueSet(newVersionIdentifier);
        final Collection<Event> eventsOriginallyFired = Events.getFired();
        Assertions.assertThat(eventsOriginallyFired).hasSize(1)
                .first()
                .isInstanceOf(RoboZonkyUpdateDetectedEvent.class);
        // check that the event has the proper version
        Assertions.assertThat(eventsOriginallyFired)
                .first()
                .matches(e -> Objects.equals(((RoboZonkyUpdateDetectedEvent) e).getNewVersion(), newVersion));
        // check that the event is not fired again since there is no change in new version
        n.valueSet(newVersionIdentifier);
        Assertions.assertThat(Events.getFired()).hasSize(1);
    }

    @Test
    public void unstableUpdate() {
        final String currentVersion = "1.0.0";
        final String newVersion = "1.0.1-beta-1";
        final VersionIdentifier newVersionIdentifier = new VersionIdentifier(currentVersion, newVersion);
        final UpdateNotification n = new UpdateNotification(currentVersion);
        // check that the event is fired
        n.valueSet(newVersionIdentifier);
        final Collection<Event> eventsOriginallyFired = Events.getFired();
        Assertions.assertThat(eventsOriginallyFired).hasSize(1)
                .first()
                .isInstanceOf(RoboZonkyExperimentalUpdateDetectedEvent.class);
        // check that the event has the proper version
        Assertions.assertThat(eventsOriginallyFired)
                .first()
                .matches(e -> Objects.equals(((RoboZonkyExperimentalUpdateDetectedEvent) e).getNewVersion(),
                                             newVersion));
        // check that the event is not fired again since there is no change in new version
        n.valueSet(newVersionIdentifier);
        Assertions.assertThat(Events.getFired()).hasSize(1);
    }

    @Test
    public void stableDoNotUpdateOlder() {
        final String currentVersion = "1.0.1";
        final String newVersion = "1.0.0";
        final VersionIdentifier newVersionIdentifier = new VersionIdentifier(newVersion);
        final UpdateNotification n = new UpdateNotification(currentVersion);
        // check that the event is not fired
        n.valueSet(newVersionIdentifier);
        final Collection<Event> eventsOriginallyFired = Events.getFired();
        Assertions.assertThat(eventsOriginallyFired).isEmpty();
        // check that the event is still not fired when we submit the current version again
        n.valueSet(new VersionIdentifier(currentVersion));
        Assertions.assertThat(Events.getFired()).isEmpty();
    }
}
