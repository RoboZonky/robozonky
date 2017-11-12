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

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Consumer;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.SessionInfo;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.util.LocalhostAddress;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractEmailingListener<T extends Event> implements EventListener<T> {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final Counter emailsOfThisType;
    private final ListenerSpecificNotificationProperties properties;
    private final Collection<Consumer<T>> finishers = new LinkedHashSet<>(0);

    public AbstractEmailingListener(final ListenerSpecificNotificationProperties properties) {
        this.properties = properties;
        this.emailsOfThisType = new Counter(this.getClass().getSimpleName(),
                                            properties.getListenerSpecificHourlyEmailLimit());
        this.registerFinisher(evt -> { // increase spam-prevention counters
            emailsOfThisType.increase();
            this.properties.getGlobalCounter().increase();
        });
    }

    private static Email createNewEmail(final NotificationProperties properties) throws EmailException {
        final Email email = new SimpleEmail();
        email.setCharset(Defaults.CHARSET.displayName());
        email.setHostName(properties.getSmtpHostname());
        email.setSmtpPort(properties.getSmtpPort());
        email.setStartTLSRequired(properties.isStartTlsRequired());
        email.setSSLOnConnect(properties.isSslOnConnectRequired());
        email.setAuthentication(properties.getSmtpUsername(), properties.getSmtpPassword());
        final String localhostAddress =
                LocalhostAddress.INSTANCE.getLatest(Duration.ofSeconds(1)).orElse("unknown host");
        email.setFrom(properties.getSender(), "RoboZonky @ " + localhostAddress);
        email.addTo(properties.getRecipient());
        return email;
    }

    protected void registerFinisher(final Consumer<T> finisher) {
        this.finishers.add(finisher);
    }

    int countFinishers() {
        return this.finishers.size();
    }

    boolean shouldSendEmail(final T event) {
        return this.properties.getGlobalCounter().allow() && this.emailsOfThisType.allow();
    }

    abstract String getSubject(final T event);

    abstract String getTemplateFileName();

    protected Map<String, Object> getData(final T event) {
        return Collections.emptyMap();
    }

    Map<String, Object> getData(final T event, final SessionInfo sessionInfo) {
        return new HashMap<String, Object>(this.getData(event)) {{
            put("session", new HashMap<String, Object>() {{
                put("userName", Util.obfuscateEmailAddress(sessionInfo.getUserName()));
                put("userAgent", sessionInfo.getUserAgent());
            }});
        }};
    }

    @Override
    public void handle(final T event, final SessionInfo sessionInfo) {
        if (!this.shouldSendEmail(event)) {
            LOGGER.debug("Will not send e-mail.");
        } else {
            try {
                final Email email = AbstractEmailingListener.createNewEmail(properties);
                email.setSubject(this.getSubject(event));
                email.setMsg(TemplateProcessor.INSTANCE.process(this.getTemplateFileName(),
                                                                this.getData(event, sessionInfo)));
                LOGGER.debug("Will send '{}' from {} to {} through {}:{} as {}.", email.getSubject(),
                             email.getFromAddress(), email.getToAddresses(), email.getHostName(), email.getSmtpPort(),
                             properties.getSmtpUsername());
                email.send();
                // perform finishers after the e-mail has been sent
                finishers.forEach(f -> {
                    try {
                        f.accept(event);
                    } catch (final Exception ex) {
                        LOGGER.trace("Finisher failed.", ex);
                    }
                });
            } catch (final Exception ex) {
                throw new RuntimeException("Failed processing event.", ex);
            }
        }
    }
}
