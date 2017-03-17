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

import java.io.File;
import java.util.Properties;

import com.github.triceo.robozonky.notifications.RefreshableNotificationProperties;

public class RefreshableEmailNotificationProperties extends RefreshableNotificationProperties<EmailNotificationProperties> {

    static final File DEFAULT_CONFIG_FILE_LOCATION = new File("robozonky-notifications-email.cfg");
    public static final String CONFIG_FILE_LOCATION_PROPERTY = "robozonky.notifications.email.config.file";

    @Override
    protected EmailNotificationProperties newNotificationProperties(final Properties properties) {
        return new EmailNotificationProperties(properties);
    }

    @Override
    protected String getConfigFileLocationPropertyName() {
        return RefreshableEmailNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY;
    }

    @Override
    protected File getDefaultConfigFileLocation() {
        return RefreshableEmailNotificationProperties.DEFAULT_CONFIG_FILE_LOCATION;
    }
}
