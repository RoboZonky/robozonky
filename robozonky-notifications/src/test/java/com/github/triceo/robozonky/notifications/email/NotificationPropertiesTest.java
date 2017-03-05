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
    private static final URL WINDOWS_ENCODED =
            NotificationPropertiesTest.class.getResource("notifications-windows-encoding.cfg");

    @Rule
    public final ClearSystemProperties myPropertyIsCleared =
            new ClearSystemProperties(EmailListenerService.CONFIG_FILE_LOCATION_PROPERTY);

    @Before
    public void prepareBackupConfig() throws Exception {
        Files.write(NotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.toPath(), "enabled = false".getBytes());
    }

    @After
    public void deleteBackupConfig() {
        NotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.delete();
    }

    @Test
    public void localHostAddress() {
        System.setProperty(EmailListenerService.CONFIG_FILE_LOCATION_PROPERTY,
                NotificationPropertiesTest.WINDOWS_ENCODED.toString());
        final Optional<String> contents = NotificationProperties.getPropertiesContents();
        final NotificationProperties np = NotificationProperties.getProperties(contents.get()).get();
        Assertions.assertThat(np.getLocalHostAddress()).isNotEmpty();
    }

    @Test
    public void wrongPropertiesUrlReadsPropertyFile() {
        System.setProperty(EmailListenerService.CONFIG_FILE_LOCATION_PROPERTY, "wrongprotocol://somewhere");
        final Optional<String> contents = NotificationProperties.getPropertiesContents();
        Assertions.assertThat(contents).isPresent();
        final Optional<NotificationProperties> np = NotificationProperties.getProperties(contents.get());
        Assertions.assertThat(np).isPresent();
        Assertions.assertThat(np.get().isEnabled()).isFalse();
    }

    @Test
    public void correctUrlIgnoresPropertyFile() {
        System.setProperty(EmailListenerService.CONFIG_FILE_LOCATION_PROPERTY,
                NotificationPropertiesTest.CONFIG_ENABLED.toString());
        final Optional<String> contents = NotificationProperties.getPropertiesContents();
        Assertions.assertThat(contents).isPresent();
        final Optional<NotificationProperties> np = NotificationProperties.getProperties(contents.get());
        Assertions.assertThat(np).isPresent();
        Assertions.assertThat(np.get().isEnabled()).isTrue();
    }

    @Test
    public void windowsEncoding() {
        System.setProperty(EmailListenerService.CONFIG_FILE_LOCATION_PROPERTY,
                NotificationPropertiesTest.WINDOWS_ENCODED.toString());
        final Optional<String> contents = NotificationProperties.getPropertiesContents();
        Assertions.assertThat(contents).isPresent();
        final Optional<NotificationProperties> np = NotificationProperties.getProperties(contents.get());
        Assertions.assertThat(np).isPresent();
        Assertions.assertThat(np.get().isEnabled()).isTrue();
    }

    @Test
    public void equals() {
        System.setProperty(EmailListenerService.CONFIG_FILE_LOCATION_PROPERTY,
                NotificationPropertiesTest.CONFIG_ENABLED.toString());
        final Optional<String> contents = NotificationProperties.getPropertiesContents();
        Assertions.assertThat(contents).isPresent();
        final NotificationProperties props1 = NotificationProperties.getProperties(contents.get()).get();
        Assertions.assertThat(props1).isEqualTo(props1);
        final NotificationProperties props2 = NotificationProperties.getProperties(contents.get()).get();
        Assertions.assertThat(props1).isNotSameAs(props2).isEqualTo(props2);
        Assertions.assertThat(props2).isEqualTo(props1);
        System.setProperty(EmailListenerService.CONFIG_FILE_LOCATION_PROPERTY,
                NotificationPropertiesTest.WINDOWS_ENCODED.toString());
        final Optional<String> contents2 = NotificationProperties.getPropertiesContents();
        final NotificationProperties props3 = NotificationProperties.getProperties(contents2.get()).get();
        Assertions.assertThat(props3).isNotEqualTo(props1);
    }

}
