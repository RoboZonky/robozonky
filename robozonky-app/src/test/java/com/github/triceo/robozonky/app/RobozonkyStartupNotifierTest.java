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

package com.github.triceo.robozonky.app;

import java.util.Optional;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.triceo.robozonky.app.notifications.Events;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RobozonkyStartupNotifierTest {

    @Test
    public void properEventsFired() {
        final RoboZonkyStartupNotifier rzsn = new RoboZonkyStartupNotifier();
        final Optional<Consumer<ReturnCode>> result = rzsn.get();
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(Events.getFired()).last().isInstanceOf(RoboZonkyInitializedEvent.class);
        result.get().accept(ReturnCode.OK);
        Assertions.assertThat(Events.getFired()).last().isInstanceOf(RoboZonkyEndingEvent.class);
    }

}
