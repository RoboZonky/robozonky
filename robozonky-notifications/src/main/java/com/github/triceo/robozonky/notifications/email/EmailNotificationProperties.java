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

import java.util.Properties;

import com.github.triceo.robozonky.internal.api.Defaults;
import com.github.triceo.robozonky.notifications.NotificationProperties;

class EmailNotificationProperties extends NotificationProperties {

    protected static final String HOURLY_LIMIT = "hourlyMaxEmails";

    protected static String getCompositePropertyName(final SupportedListener listener, final String property) {
        return listener.getLabel() + "." + property;
    }

    private String localHostAddress;

    EmailNotificationProperties(final EmailNotificationProperties source) {
        this(source.getProperties());
    }

    EmailNotificationProperties(final Properties source) {
        super(source);
    }

    public synchronized String getLocalHostAddress() {
        if (localHostAddress == null) {
            // lazy init so that the remote request penalty is not incurred needlessly
            localHostAddress = Defaults.getHostAddress();
        }
        return localHostAddress;
    }

    public String getSender() {
        return this.getStringValue("from", "noreply@robozonky.cz");
    }

    public String getRecipient() {
        return this.getStringValue("to", "");
    }

    public boolean isStartTlsRequired() {
        return this.getBooleanValue("smtp.requiresStartTLS", false);
    }

    public boolean isSslOnConnectRequired() {
        return this.getBooleanValue("smtp.requiresSslOnConnect", false);
    }

    public String getSmtpUsername() {
        return this.getStringValue("smtp.username", this.getRecipient());
    }

    public String getSmtpPassword() {
        return this.getStringValue("smtp.password", "");
    }

    public String getSmtpHostname() {
        return this.getStringValue("smtp.hostname", "localhost");
    }

    public int getSmtpPort() {
        return this.getIntValue("smtp.port", 25);
    }

    public boolean isListenerEnabled(final SupportedListener listener) {
        if (listener == SupportedListener.TESTING) {
            return true;
        } else {
            final String propName = EmailNotificationProperties.getCompositePropertyName(listener, "enabled");
            return this.isEnabled() && this.getBooleanValue(propName, false);
        }
    }

    @Override
    protected int getGlobalHourlyLimit() {
        final int val = this.getIntValue(EmailNotificationProperties.HOURLY_LIMIT)
                .orElse(Integer.MAX_VALUE);
        if (val < 0) {
            return Integer.MAX_VALUE;
        } else {
            return val;
        }
    }

}
