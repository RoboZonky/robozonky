/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.notifications;

import com.github.robozonky.api.SessionInfo;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class EmailHandler extends AbstractTargetHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailHandler.class);

    public EmailHandler(final ConfigStorage config) {
        super(config, Target.EMAIL);
    }

    private String getSender() {
        return config.read(target, "from", "noreply@robozonky.cz");
    }

    private String getRecipient() {
        return config.read(target, "to", "");
    }

    private boolean isAuthenticationRequired() { // for backwards compatibility reasons, defaults to true
        return config.readBoolean(target, "smtp.requiresAuthentication", true);
    }

    private boolean isStartTlsRequired() {
        return config.readBoolean(target, "smtp.requiresStartTLS", false);
    }

    private boolean isSslOnConnectRequired() {
        return config.readBoolean(target, "smtp.requiresSslOnConnect", false);
    }

    private String getSmtpUsername() {
        return config.read(target, "smtp.username", this.getSender());
    }

    private String getSmtpPassword() {
        return config.read(target, "smtp.password", "");
    }

    private String getSmtpHostname() {
        return config.read(target, "smtp.hostname", "localhost");
    }

    private int getSmtpPort() {
        return config.readInt(target, "smtp.port", 25);
    }

    private HtmlEmail createNewEmail(final SessionInfo session) throws EmailException {
        final HtmlEmail email = new HtmlEmail();
        email.setHostName(getSmtpHostname());
        email.setSmtpPort(getSmtpPort());
        email.setStartTLSRequired(isStartTlsRequired());
        email.setSSLOnConnect(isSslOnConnectRequired());
        if (isAuthenticationRequired()) {
            final String username = getSmtpUsername();
            LOGGER.debug("Will contact SMTP server as '{}'.", username);
            email.setAuthentication(getSmtpUsername(), getSmtpPassword());
        } else {
            LOGGER.debug("Will contact SMTP server anonymously.");
        }
        final String sessionName = session.getName().map(n -> "RoboZonky '" + n + "'").orElse("RoboZonky");
        email.setFrom(getSender(), sessionName);
        email.addTo(getRecipient());
        return email;
    }

    @Override
    public void send(final SessionInfo sessionInfo, final String subject,
                     final String message, final String fallbackMessage) throws Exception {
        final HtmlEmail email = createNewEmail(sessionInfo);
        email.setSubject(subject);
        email.setHtmlMsg(message);
        email.setTextMsg(fallbackMessage);
        LOGGER.debug("Will send '{}' from {} to {} through {}:{} as {}.", email.getSubject(),
                     email.getFromAddress(), email.getToAddresses(), email.getHostName(), email.getSmtpPort(),
                     getSmtpUsername());
        email.send();
    }
}
