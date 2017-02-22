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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractEmailingListener<T extends Event> implements EventListener<T> {

    protected static String stackTraceToString(final Throwable t) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    private static Email createNewEmail(final NotificationProperties properties) throws EmailException {
        final Email email = new SimpleEmail();
        email.setCharset(Defaults.CHARSET.displayName());
        email.setHostName(properties.getSmtpHostname());
        email.setSmtpPort(properties.getSmtpPort());
        email.setStartTLSRequired(properties.isStartTlsRequired());
        email.setSSLOnConnect(properties.isSslOnConnectRequired());
        email.setAuthentication(properties.getSmtpUsername(), properties.getSmtpPassword());
        email.setFrom(properties.getSender(), "RoboZonky @ " + properties.getLocalHostAddress());
        email.addTo(properties.getRecipient());
        return email;
    }

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final EmailCounter emailsOfThisType;
    private final ListenerSpecificNotificationProperties properties;

    public AbstractEmailingListener(final ListenerSpecificNotificationProperties properties) {
        this.properties = properties;
        this.emailsOfThisType = new EmailCounter(this.getClass().getSimpleName(),
                properties.getListenerSpecificHourlyEmailLimit());
    }

    boolean shouldSendEmail(final T event) {
        return this.properties.getGlobalEmailCounter().allowEmail() && this.emailsOfThisType.allowEmail();
    }

    abstract String getSubject(final T event);

    abstract String getTemplateFileName();

    Map<String, Object> getData(final T event) {
        return Collections.emptyMap();
    }

    @Override
    public void handle(final T event) {
        if (!this.shouldSendEmail(event)) {
            LOGGER.debug("Will not send e-mail.");
            return;
        } else try {
            final Email email = AbstractEmailingListener.createNewEmail(properties);
            email.setSubject(this.getSubject(event));
            email.setMsg(TemplateProcessor.INSTANCE.process(this.getTemplateFileName(), this.getData(event)));
            LOGGER.debug("Will send '{}' from {} to {} through {}:{} as {}.",
                    email.getSubject(), email.getFromAddress(), email.getToAddresses(), email.getHostName(),
                    email.getSmtpPort(), properties.getSmtpUsername());
            email.send();
            emailsOfThisType.emailSent();
            this.properties.getGlobalEmailCounter().emailSent();
        } catch (final Exception ex) {
            throw new RuntimeException("Failed processing event.", ex);
        }

    }

}
