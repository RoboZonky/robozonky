/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.installer;

import java.util.UUID;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class EmailSettingsValidatorTest {

    private static final GreenMail EMAIL = new GreenMail(getServerSetup());
    private static final Logger LOGGER = LogManager.getLogger(EmailSettingsValidatorTest.class);

    private static ServerSetup getServerSetup() {
        final ServerSetup setup = ServerSetupTest.SMTP;
        setup.setServerStartupTimeout(5000);
        setup.setVerbose(true);
        return setup;
    }

    @BeforeEach
    void startEmailing() {
        EMAIL.start();
        LOGGER.info("Started e-mailing.");
    }

    @AfterEach
    void stopEmailing() {
        LOGGER.info("Stopping e-mailing.");
        try {
            EMAIL.purgeEmailFromAllMailboxes();
            EMAIL.stop();
        } catch (final Exception ex) {
            LOGGER.warn("Failed stopping e-mail server.", ex);
        }
    }

    @Test
    void messages() {
        final DataValidator validator = new EmailSettingsValidator();
        assertSoftly(softly -> {
            softly.assertThat(validator.getDefaultAnswer()).isFalse();
            softly.assertThat(validator.getWarningMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEqualTo(validator.getWarningMessageId());
        });
    }

    @Test
    void mailSent() {
        final InstallData data = mock(InstallData.class);
        when(data.getVariable(eq(Variables.ZONKY_USERNAME.getKey())))
                .thenReturn("someone@somewhere.cz");
        when(data.getVariable(eq(Variables.SMTP_PORT.getKey())))
                .thenReturn(String.valueOf(EMAIL.getSmtp().getPort()));
        when(data.getVariable(eq(Variables.SMTP_HOSTNAME.getKey())))
                .thenReturn(String.valueOf(EMAIL.getSmtp().getBindTo()));
        when(data.getVariable(eq(Variables.SMTP_TO.getKey())))
                .thenReturn("recipient@server.cz");
        when(data.getVariable(eq(Variables.SMTP_USERNAME.getKey())))
                .thenReturn("sender@server.cz");
        when(data.getVariable(eq(Variables.SMTP_PASSWORD.getKey())))
                .thenReturn(UUID.randomUUID().toString());
        final DataValidator validator = new EmailSettingsValidator();
        final DataValidator.Status result = validator.validateData(data);
        assertThat(result).isEqualTo(DataValidator.Status.OK);
        assertThat(EMAIL.getReceivedMessages()).hasSize(1);
    }

    @Test
    void mailFailed() {
        final InstallData data = mock(InstallData.class);
        when(data.getVariable(eq(Variables.SMTP_PORT.getKey())))
                .thenReturn(String.valueOf(EMAIL.getSmtp().getPort()));
        when(data.getVariable(eq(Variables.SMTP_HOSTNAME.getKey())))
                .thenReturn(String.valueOf(EMAIL.getSmtp().getBindTo()));
        when(data.getVariable(eq(Variables.SMTP_USERNAME.getKey())))
                .thenReturn("sender@server.cz");
        when(data.getVariable(eq(Variables.SMTP_PASSWORD.getKey())))
                .thenReturn(UUID.randomUUID().toString());
        final DataValidator validator = new EmailSettingsValidator();
        final DataValidator.Status result = validator.validateData(data);
        assertThat(result).isEqualTo(DataValidator.Status.WARNING);
        assertThat(EMAIL.getReceivedMessages()).hasSize(0);
    }
}
