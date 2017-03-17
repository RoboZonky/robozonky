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

import java.util.Properties;

import com.github.triceo.robozonky.notifications.NotificationProperties;

class FileNotificationProperties extends NotificationProperties {

    protected static final String HOURLY_LIMIT = "hourlyMaxFiles";

    protected static String getCompositePropertyName(final SupportedListener listener, final String property) {
        return listener.getLabel() + "." + property;
    }

    FileNotificationProperties(final FileNotificationProperties source) {
        this(source.getProperties());
    }

    FileNotificationProperties(final Properties source) {
        super(source);
    }

    public boolean isListenerEnabled(final SupportedListener listener) {
        return this.getBooleanValue(FileNotificationProperties.getCompositePropertyName(listener, "enabled"), false);
    }

    @Override
    public int getGlobalHourlyLimit() {
        final int val = this.getIntValue(FileNotificationProperties.HOURLY_LIMIT)
                .orElse(Integer.MAX_VALUE);
        if (val < 0) {
            return Integer.MAX_VALUE;
        } else {
            return val;
        }
    }

}
