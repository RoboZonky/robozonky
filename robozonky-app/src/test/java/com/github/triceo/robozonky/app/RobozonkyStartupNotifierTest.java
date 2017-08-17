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

package com.github.triceo.robozonky.app;

import java.util.Optional;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyInitializedEvent;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RobozonkyStartupNotifierTest extends AbstractEventsAndStateLeveragingTest {

    @Test
    public void properEventsFired() {
        final String username = "username";
        final RoboZonkyStartupNotifier rzsn = new RoboZonkyStartupNotifier(username);
        final Optional<Consumer<ShutdownHook.Result>> result = rzsn.get();
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(Events.getFired()).last().isInstanceOf(RoboZonkyInitializedEvent.class);
        final ShutdownHook.Result r = new ShutdownHook.Result(ReturnCode.OK, null);
        result.get().accept(r);
        Assertions.assertThat(Events.getFired()).last().isInstanceOf(RoboZonkyEndingEvent.class);
        final ShutdownHook.Result r2 = new ShutdownHook.Result(ReturnCode.ERROR_WRONG_PARAMETERS, null);
        result.get().accept(r2);
        Assertions.assertThat(Events.getFired()).last().isInstanceOf(RoboZonkyCrashedEvent.class);
    }
}
