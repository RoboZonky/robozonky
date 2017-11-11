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

import com.github.robozonky.internal.api.Settings;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.After;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

public abstract class AbstractEventLeveragingRoboZonkyTest extends AbstractRoboZonkyTest {

    @Rule
    public final ProvideSystemProperty property =
            new ProvideSystemProperty(Settings.Key.DEBUG_ENABLE_EVENT_STORAGE.getName(), "true");

    @After
    public void clear() {
        Events.getFired().clear();
        Events.INSTANCE.registries.clear();
    }
}
