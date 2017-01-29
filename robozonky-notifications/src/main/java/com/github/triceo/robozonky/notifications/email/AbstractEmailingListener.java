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

import java.util.Map;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will convert an investment into a timestamped file informing user of when the investment was made.
 */
abstract class AbstractEmailingListener<T extends Event> implements EventListener<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEmailingListener.class);

    private static Email createNewEmail(final NotificationProperties properties) throws EmailException {
        final Email email = new SimpleEmail();
        email.setCharset(Defaults.CHARSET.displayName());
        email.setHostName(properties.getSmtpHostname());
        email.setSmtpPort(properties.getSmtpPort());
        email.setStartTLSRequired(properties.isStartTlsRequired());
        email.setSSLOnConnect(properties.isSslOnConnectRequired());
        email.setAuthentication(properties.getSmtpUsername(), properties.getSmtpPassword());
        email.setFrom(properties.getSender(), "RoboZonky @ " + Defaults.ROBOZONKY_HOST_ADDRESS);
        email.addTo(properties.getRecipient());
        return email;
    }

    private final ListenerSpecificNotificationProperties properties;
    private final TemplateProcessor templates = new TemplateProcessor();

    public AbstractEmailingListener(final ListenerSpecificNotificationProperties properties) {
        this.properties = properties;
    }

    abstract boolean shouldSendEmail(final T event);

    abstract String getSubject(final T event);

    abstract String getTemplateFileName();

    abstract Map<String, Object> getData(final T event);

    @Override
    public void handle(final T event) {
        if (!this.shouldSendEmail(event)) {
            return;
        } else try {
            final Email email = AbstractEmailingListener.createNewEmail(properties);
            email.setSubject(this.getSubject(event));
            email.setMsg(this.templates.process(this.getTemplateFileName(), this.getData(event)));
            AbstractEmailingListener.LOGGER.debug("Will send '{}' from {} to {} through {}:{} as {}.",
                    email.getSubject(), email.getFromAddress(), email.getToAddresses(), email.getHostName(),
                    email.getSmtpPort(), properties.getSmtpUsername());
            email.send();
        } catch (final Exception ex) {
            throw new RuntimeException("Failed processing event.", ex);
        }

    }

}
