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

package com.github.robozonky.app;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.internal.api.Settings;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractEventLeveragingTest extends AbstractRoboZonkyTest {

    private final Collection<Event> previouslyExistingEvents = new LinkedHashSet<>(0);

    @BeforeEach
    public void enableEventDebug() {
        System.setProperty(Settings.Key.DEBUG_ENABLE_EVENT_STORAGE.getName(), "true");
    }

    @AfterEach
    public void clear() {
        Events.getFired().clear();
        Events.INSTANCE.registries.clear();
    }

    protected List<Event> getNewEvents() {
        return Events.getFired().stream()
                .filter(e -> !previouslyExistingEvents.contains(e))
                .collect(Collectors.toList());
    }

    @BeforeEach
    public void readPreexistingEvents() {
        previouslyExistingEvents.addAll(Events.getFired());
    }
}
