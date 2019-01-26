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

import java.io.File;
import java.io.IOException;

import com.github.robozonky.notifications.listeners.RoboZonkyTestingEventListener;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ConfigStorageTest {

    @Test
    void equality() throws IOException {
        final ConfigStorage cs = ConfigStorage.create(RoboZonkyTestingEventListener.class
                                                              .getResourceAsStream("notifications-enabled.cfg"));
        assertThat(cs).isEqualTo(cs);
        assertThat(cs).isNotEqualTo(null);
        assertThat(cs).isNotEqualTo("");
        final ConfigStorage cs2 = ConfigStorage.create(RoboZonkyTestingEventListener.class
                                                               .getResourceAsStream("notifications-enabled.cfg"));
        assertThat(cs).isEqualTo(cs2);
        assertThat(cs2).isEqualTo(cs);
        final ConfigStorage cs3 = ConfigStorage.create(RoboZonkyTestingEventListener.class
                                                               .getResourceAsStream(
                                                                       "notifications-enabled-spamless.cfg"));
        assertThat(cs).isNotEqualTo(cs3);
        assertThat(cs3).isNotEqualTo(cs);
    }

    @Test
    void fromFile() throws IOException {
        final ConfigStorage cs = ConfigStorage.create(File.createTempFile("robozonky-", ".tmp"));
        assertThat(cs).isNotNull();
    }
}
