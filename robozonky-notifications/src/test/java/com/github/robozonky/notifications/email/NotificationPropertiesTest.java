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

package com.github.robozonky.notifications.email;

import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Properties;

import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

class NotificationPropertiesTest extends AbstractRoboZonkyTest {

    private static final URL CONFIG_ENABLED =
            NotificationPropertiesTest.class.getResource("notifications-enabled.cfg");
    private static final URL WINDOWS_ENCODED =
            NotificationPropertiesTest.class.getResource("notifications-windows-encoding.cfg");

    private static Optional<NotificationProperties> getProperties() {
        return new RefreshableNotificationProperties().get();
    }

    @BeforeEach
    void prepareBackupConfig() throws Exception {
        Files.write(RefreshableNotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.toPath(),
                    "enabled = false".getBytes());
    }

    @AfterEach
    void deleteBackupConfig() {
        RefreshableNotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.delete();
    }

    @Test
    void wrongPropertiesUrlReadsPropertyFile() {
        System.setProperty(RefreshableNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
                           "wrongprotocol://somewhere");
        final Optional<NotificationProperties> np = NotificationPropertiesTest.getProperties();
        assertThat(np).isPresent();
        assertThat(np.get().isEnabled()).isFalse();
    }

    @Test
    void correctUrlIgnoresPropertyFile() {
        System.setProperty(RefreshableNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
                           NotificationPropertiesTest.CONFIG_ENABLED.toString());
        final Optional<NotificationProperties> np = NotificationPropertiesTest.getProperties();
        assertThat(np).isPresent();
        assertThat(np.get().isEnabled()).isTrue();
    }

    @Test
    void windowsEncoding() {
        System.setProperty(RefreshableNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
                           NotificationPropertiesTest.WINDOWS_ENCODED.toString());
        final Optional<NotificationProperties> np = NotificationPropertiesTest.getProperties();
        assertThat(np).isPresent();
        assertThat(np.get().isEnabled()).isTrue();
    }

    @Test
    void equality() {
        final Properties p = new Properties();
        final NotificationProperties n = new NotificationProperties(p);
        final NotificationProperties n2 = new NotificationProperties(p);
        assertSoftly(softly -> {
            softly.assertThat(n).isNotEqualTo(null);
            softly.assertThat(n).isNotEqualTo("some string");
            softly.assertThat(n).isEqualTo(n);
            softly.assertThat(n2).isEqualTo(n);
            softly.assertThat(n).isEqualTo(n2);
        });
    }
}
