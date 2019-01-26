/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.notifications;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.ListenerService;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.common.extensions.ListenerServiceLoader;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class NotificationListenerServiceTest extends AbstractRoboZonkyTest {

    @Test
    void noConfigs() {
        final ListenerService s = new NotificationListenerService();
        assertThat(s.findListeners(RoboZonkyTestingEvent.class)).isEmpty();
    }

    @Test
    void noValidConfigs() {
        ListenerServiceLoader.registerConfiguration(new SessionInfo("a@b.c"), "invalid-url");
        final ListenerService s = new NotificationListenerService();
        assertThat(s.findListeners(RoboZonkyTestingEvent.class)).isEmpty();
    }
}
