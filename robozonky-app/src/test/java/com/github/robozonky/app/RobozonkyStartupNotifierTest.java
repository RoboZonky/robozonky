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

package com.github.robozonky.app;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.github.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RobozonkyStartupNotifierTest extends AbstractEventLeveragingTest {

    @Test
    void properEventsFired() {
        final RoboZonkyStartupNotifier rzsn = new RoboZonkyStartupNotifier(UUID.randomUUID().toString());
        final Optional<Consumer<ShutdownHook.Result>> result = rzsn.get();
        assertThat(result).isPresent();
        assertThat(this.getEventsRequested()).last().isInstanceOf(RoboZonkyInitializedEvent.class);
        final ShutdownHook.Result r = new ShutdownHook.Result(ReturnCode.OK, null);
        result.get().accept(r);
        assertThat(this.getEventsRequested()).last().isInstanceOf(RoboZonkyEndingEvent.class);
        final ShutdownHook.Result r2 = new ShutdownHook.Result(ReturnCode.ERROR_SETUP, null);
        result.get().accept(r2);
        assertThat(this.getEventsRequested()).last().isInstanceOf(RoboZonkyCrashedEvent.class);
    }
}
