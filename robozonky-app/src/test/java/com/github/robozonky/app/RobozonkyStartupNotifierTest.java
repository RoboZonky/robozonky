/*
 * Copyright 2020 The RoboZonky Project
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

import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.*;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.app.events.AbstractEventLeveragingTest;

class RobozonkyStartupNotifierTest extends AbstractEventLeveragingTest {

    private static final SessionInfo SESSION = mockSessionInfo();

    @Test
    void properEventsFired() {
        final RoboZonkyStartupNotifier rzsn = new RoboZonkyStartupNotifier(SESSION);
        final Optional<Consumer<ReturnCode>> result = rzsn.get();
        assertThat(result).isPresent();
        assertThat(this.getEventsRequested()).last()
            .isInstanceOf(RoboZonkyInitializedEvent.class);
        final ReturnCode r = ReturnCode.OK;
        result.get()
            .accept(r);
        assertThat(this.getEventsRequested()).last()
            .isInstanceOf(RoboZonkyEndingEvent.class);
    }

    @Test
    void properBanner() {
        String lineSep = lineSeparator();
        String expected = lineSep +
                "################################################################################" + lineSep +
                "#        ____            __          _____                   __                #" + lineSep +
                "#       / __ \\  ____    / /_   ____ /__  /  ____    ____    / /__   __  __     #" + lineSep +
                "#      / /_/ / / __ \\  / __ \\ / __ \\  / /  / __ \\  / __ \\  / //_/  / / / /     #" + lineSep +
                "#     / _, _/ / /_/ / / /_/ // /_/ / / /__/ /_/ / / / / / / ,<    / /_/ /      #" + lineSep +
                "#    /_/ |_|  \\____/ /_.___/ \\____/ /____/\\____/ /_/ /_/ /_/|_|   \\__, /       #" + lineSep +
                "#                                     v6.0.0-SNAPSHOT 'Testing'  /____/        #" + lineSep +
                "#                                                                              #" + lineSep +
                "################################################################################";
        final RoboZonkyStartupNotifier rzsn = new RoboZonkyStartupNotifier(SESSION);
        assertThat(rzsn.readBanner("6.0.0-SNAPSHOT")).isEqualTo(expected);
    }
}
