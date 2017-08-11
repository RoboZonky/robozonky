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
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.SessionInfo;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.internal.api.Defaults;
import com.github.triceo.robozonky.util.LocalhostAddress;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractEmailingListener<T extends Event> implements EventListener<T> {

    private static final String AT = "@";
    private static final Pattern COMPILE = Pattern.compile("\\Q" + AbstractEmailingListener.AT + "\\E");

    private static String cutMiddle(final String text) {
        if (text.length() < 3) {
            return text;
        } else {
            return text.charAt(0) + "..." + text.charAt(text.length() - 1);
        }
    }

    private static String obfuscateEmailAddress(final String username) {
        if (username.contains(AbstractEmailingListener.AT)) {
            final String[] parts = AbstractEmailingListener.COMPILE.split(username.toLowerCase());
            return AbstractEmailingListener.cutMiddle(parts[0].trim()) + AbstractEmailingListener.AT
                    + AbstractEmailingListener.cutMiddle(parts[1].trim());
        } else {
            throw new IllegalArgumentException("Not a valid e-mail address: " + username);
        }
    }

    protected static String getLoanUrl(final Investment i) {
        // convert investment safely to URL, using dummy loan if necessary
        final Loan l = i.getLoan().orElseGet(() -> new Loan(i.getLoanId(), Defaults.MINIMUM_INVESTMENT_IN_CZK));
        return Loan.getUrlSafe(l);
    }

    static String stackTraceToString(final Throwable t) {
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
        final String localhostAddress =
                LocalhostAddress.INSTANCE.getLatest(Duration.ofSeconds(1)).orElse("unknown host");
        email.setFrom(properties.getSender(), "RoboZonky @ " + localhostAddress);
        email.addTo(properties.getRecipient());
        return email;
    }

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final Counter emailsOfThisType;
    private final ListenerSpecificNotificationProperties properties;

    public AbstractEmailingListener(final ListenerSpecificNotificationProperties properties) {
        this.properties = properties;
        this.emailsOfThisType = new Counter(this.getClass().getSimpleName(),
                                            properties.getListenerSpecificHourlyEmailLimit());
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
        final Map<String, Object> eventSpecific = this.getData(event);
        final Map<String, Object> userInfoMap = new HashMap<>();
        userInfoMap.put("userName", AbstractEmailingListener.obfuscateEmailAddress(sessionInfo.getUserName()));
        userInfoMap.put("userAgent", sessionInfo.getUserAgent());
        final Map<String, Object> result = new HashMap<>(eventSpecific);
        result.put("session", userInfoMap);
        return result;
    }

    @Override
    public void handle(final T event, final SessionInfo sessionInfo) {
        if (!this.shouldSendEmail(event)) {
            LOGGER.debug("Will not send e-mail.");
        } else {
            try {
                final Email email = AbstractEmailingListener.createNewEmail(properties);
                email.setSubject(this.getSubject(event));
                email.setMsg(
                        TemplateProcessor.INSTANCE.process(this.getTemplateFileName(),
                                                           this.getData(event, sessionInfo)));
                LOGGER.debug("Will send '{}' from {} to {} through {}:{} as {}.",
                             email.getSubject(), email.getFromAddress(), email.getToAddresses(), email.getHostName(),
                             email.getSmtpPort(), properties.getSmtpUsername());
                email.send();
                emailsOfThisType.increase();
                this.properties.getGlobalCounter().increase();
            } catch (final Exception ex) {
                throw new RuntimeException("Failed processing event.", ex);
            }
        }
    }
}
