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

import java.util.Collections;
import java.util.Map;
import javax.mail.internet.MimeMessage;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.notifications.listeners.RoboZonkyTestingEventListener;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class EmailHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailHandlerTest.class);
    private static final GreenMail EMAIL = new GreenMail(getServerSetup());

    private static ServerSetup getServerSetup() {
        final ServerSetup setup = ServerSetupTest.SMTP;
        setup.setServerStartupTimeout(5000);
        setup.setVerbose(true);
        return setup;
    }

    @Test
    void sendsEmails() throws Exception {
        final int originalMessages = EMAIL.getReceivedMessages().length;
        final ConfigStorage cs = ConfigStorage.create(RoboZonkyTestingEventListener.class
                                                              .getResourceAsStream("notifications-enabled.cfg"));
        final EmailHandler h = new EmailHandler(cs);
        final String subject = "A", body = "B";
        h.offer(new Submission() {
            @Override
            public SessionInfo getSessionInfo() {
                return new SessionInfo("someone@somewhere.cz");
            }

            @Override
            public SupportedListener getSupportedListener() {
                return SupportedListener.TESTING;
            }

            @Override
            public Map<String, Object> getData() {
                return Collections.emptyMap();
            }

            @Override
            public String getSubject() {
                return subject;
            }

            @Override
            public String getMessage(final Map<String, Object> data) {
                return body;
            }

            @Override
            public String getFallbackMessage(final Map<String, Object> data) {
                return body;
            }
        });
        assertThat(EMAIL.getReceivedMessages()).hasSize(originalMessages + 1);
        final MimeMessage m = EMAIL.getReceivedMessages()[originalMessages];
        assertThat(m.getSubject()).isNotNull().isEqualTo(subject);
        assertThat(m.getFrom()[0].toString()).contains("user@seznam.cz");
    }

    @BeforeEach
    void startEmailing() {
        EMAIL.start();
        EMAIL.setUser("user@seznam.cz", "user@seznam.cz", "pass").create();
        LOGGER.info("Started e-mailing.");
    }

    @AfterEach
    void stopEmailing() {
        LOGGER.info("Stopping e-mailing.");
        try {
            EMAIL.stop();
        } catch (final Exception ex) {
            LOGGER.warn("Failed stopping e-mail server.", ex);
        } finally {
            EMAIL.reset();
        }
    }
}
