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

package com.github.triceo.robozonky.notifications.files;

import java.io.File;
import java.util.Optional;
import java.util.Properties;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.notifications.RefreshableNotificationProperties;

class RefreshableFileNotificationProperties extends RefreshableNotificationProperties<FileNotificationProperties> {

    static final File DEFAULT_CONFIG_FILE_LOCATION = new File("robozonky-notifications-file.cfg");
    public static final String CONFIG_FILE_LOCATION_PROPERTY = "robozonky.notifications.files.config.file";

    @Override
    protected FileNotificationProperties newNotificationProperties(final Properties properties) {
        return new FileNotificationProperties(properties);
    }

    @Override
    protected String getConfigFileLocationPropertyName() {
        return RefreshableFileNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY;
    }

    @Override
    protected File getDefaultConfigFileLocation() {
        return RefreshableFileNotificationProperties.DEFAULT_CONFIG_FILE_LOCATION;
    }

    @Override
    public Optional<Refreshable<?>> getDependedOn() {
        return Optional.empty();
    }

}
