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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

public abstract class AbstractEventLeveragingRoboZonkyTest extends AbstractRoboZonkyTest {

    @Rule
    public final ProvideSystemProperty property =
            new ProvideSystemProperty(Settings.Key.DEBUG_ENABLE_EVENT_STORAGE.getName(), "true");

    private Collection<Event> previouslyExistingEvents = new LinkedHashSet<>();

    @After
    public void clear() {
        Events.getFired().clear();
        Events.INSTANCE.registries.clear();
    }

    protected List<Event> getNewEvents() {
        return Events.getFired().stream()
                .filter(e -> !previouslyExistingEvents.contains(e))
                .collect(Collectors.toList());
    }

    @Before
    public void readPreexistingEvents() {
        previouslyExistingEvents.addAll(Events.getFired());
    }
}
