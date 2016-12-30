/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.notifications.email;

import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ClearSystemProperties;

public class NotificationPropertiesTest {

    private static final URL CONFIG_ENABLED =
            NotificationPropertiesTest.class.getResource("notifications-enabled.cfg");

    @Rule
    public final ClearSystemProperties myPropertyIsCleared =
            new ClearSystemProperties(NotificationProperties.CONFIG_FILE_LOCATION_PROPERTY);

    @Before
    public void prepareBackupConfig() throws Exception {
        Files.write(NotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.toPath(), "enabled = false".getBytes());
    }

    @After
    public void deleteBackupConfig() {
        NotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.delete();
    }

    @Test
    public void wrongPropertiesUrlReadsPropertyFile() {
        System.setProperty(NotificationProperties.CONFIG_FILE_LOCATION_PROPERTY, "wrongprotocol://somewhere");
        final Optional<NotificationProperties> np = NotificationProperties.getProperties();
        Assertions.assertThat(np).isPresent();
        Assertions.assertThat(np.get().isEnabled()).isFalse();
    }

    @Test
    public void correctUrlIgnoresPropertyFile() {
        System.setProperty(NotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
                NotificationPropertiesTest.CONFIG_ENABLED.toString());
        final Optional<NotificationProperties> np = NotificationProperties.getProperties();
        Assertions.assertThat(np).isPresent();
        Assertions.assertThat(np.get().isEnabled()).isTrue();
    }

}
