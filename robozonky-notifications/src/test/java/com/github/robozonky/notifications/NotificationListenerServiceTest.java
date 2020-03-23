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

package com.github.robozonky.notifications;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.notifications.ListenerService;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.internal.extensions.ListenerServiceLoader;
import com.github.robozonky.test.AbstractRoboZonkyTest;

class NotificationListenerServiceTest extends AbstractRoboZonkyTest {

    @Test
    void noConfigs() {
        final ListenerService s = new NotificationListenerService();
        assertThat(s.findListeners(SESSION, RoboZonkyTestingEvent.class))
            .hasSize(1)
            .first()
            .returns(Optional.empty(), Supplier::get);
    }

    @Test
    void noValidConfigs() {
        ListenerServiceLoader.registerConfiguration(SESSION, "invalid-url");
        final ListenerService s = new NotificationListenerService();
        assertThat(s.findListeners(SESSION, RoboZonkyTestingEvent.class))
            .hasSize(1)
            .first()
            .returns(Optional.empty(), Supplier::get);
    }

    @Test
    void validConfigs() throws IOException {
        final Path path = Files.createTempFile("robozonky-", ".cfg");
        try (InputStream s = getClass().getResourceAsStream("listeners/notifications-enabled.cfg")) {
            Files.write(path, s.readAllBytes());
        }
        ListenerServiceLoader.registerConfiguration(SESSION, path.toUri()
            .toURL());
        final ListenerService s = new NotificationListenerService();
        assertThat(s.findListeners(SESSION, RoboZonkyTestingEvent.class))
            .hasSize(1)
            .first()
            .extracting(Supplier::get)
            .extracting(Optional::isPresent)
            .isEqualTo(true);
    }

}
