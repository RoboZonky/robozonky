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

class SimpleEmailServerValidatorTest {

    private static final GreenMail EMAIL = new GreenMail(getServerSetup());
    private static final Logger LOGGER = LogManager.getLogger(SimpleEmailServerValidatorTest.class);
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String USERNAME = "sender@server.cz";

    private static ServerSetup getServerSetup() {
        final ServerSetup setup = ServerSetupTest.SMTP;
        setup.setServerStartupTimeout(5000);
        setup.setVerbose(true);
        return setup;
    }

    @BeforeEach
    void startEmailing() {
        EMAIL.start();
        EMAIL.setUser(USERNAME, USERNAME, PASSWORD);
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
        final DataValidator validator = new SimpleEmailServerValidator();
        assertSoftly(softly -> {
            softly.assertThat(validator.getDefaultAnswer()).isFalse();
            softly.assertThat(validator.getWarningMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEqualTo(validator.getWarningMessageId());
        });
    }

    @Test
    void configuresGmail() {
        final InstallData data = mock(InstallData.class);
        when(data.getVariable(eq(Variables.EMAIL_CONFIGURATION_TYPE.getKey()))).thenReturn("gmail.com");
        final DataValidator validator = new SimpleEmailServerValidator();
        validator.validateData(data); // will fail, but that's OK here; we only care about the configuration aspect
        verify(data).setVariable(eq(Variables.IS_EMAIL_ENABLED.getKey()), eq("true"));
        verify(data).setVariable(eq(Variables.SMTP_AUTH.getKey()), eq("true"));
        verify(data).setVariable(eq(Variables.SMTP_IS_SSL.getKey()), eq("true"));
        verify(data).setVariable(eq(Variables.SMTP_IS_TLS.getKey()), eq("false"));
        verify(data).setVariable(eq(Variables.SMTP_PORT.getKey()), eq("465"));
        verify(data).setVariable(eq(Variables.SMTP_HOSTNAME.getKey()), eq("smtp.gmail.com"));
    }

    @Test
    void configuresSeznam() {
        final InstallData data = mock(InstallData.class);
        when(data.getVariable(eq(Variables.EMAIL_CONFIGURATION_TYPE.getKey()))).thenReturn("seznam.cz");
        final DataValidator validator = new SimpleEmailServerValidator();
        validator.validateData(data); // will fail, but that's OK here; we only care about the configuration aspect
        verify(data).setVariable(eq(Variables.IS_EMAIL_ENABLED.getKey()), eq("true"));
        verify(data).setVariable(eq(Variables.SMTP_AUTH.getKey()), eq("true"));
        verify(data).setVariable(eq(Variables.SMTP_IS_SSL.getKey()), eq("true"));
        verify(data).setVariable(eq(Variables.SMTP_IS_TLS.getKey()), eq("false"));
        verify(data).setVariable(eq(Variables.SMTP_PORT.getKey()), eq("465"));
        verify(data).setVariable(eq(Variables.SMTP_HOSTNAME.getKey()), eq("smtp.seznam.cz"));
    }

    @Test
    void connected() {
        final InstallData data = mock(InstallData.class);
        when(data.getVariable(eq(Variables.EMAIL_CONFIGURATION_TYPE.getKey()))).thenReturn("gmail.com");
        when(data.getVariable(eq(Variables.ZONKY_USERNAME.getKey())))
                .thenReturn("someone@somewhere.cz");
        when(data.getVariable(eq(Variables.SMTP_AUTH.getKey()))).thenReturn("true");
        when(data.getVariable(eq(Variables.SMTP_IS_SSL.getKey()))).thenReturn("false");
        when(data.getVariable(eq(Variables.SMTP_IS_TLS.getKey()))).thenReturn("false");
        when(data.getVariable(eq(Variables.SMTP_USERNAME.getKey()))).thenReturn(USERNAME);
        when(data.getVariable(eq(Variables.SMTP_PASSWORD.getKey()))).thenReturn(PASSWORD);
        when(data.getVariable(eq(Variables.SMTP_PORT.getKey())))
                .thenReturn(String.valueOf(EMAIL.getSmtp().getPort()));
        when(data.getVariable(eq(Variables.SMTP_HOSTNAME.getKey())))
                .thenReturn(String.valueOf(EMAIL.getSmtp().getBindTo()));
        when(data.getVariable(eq(Variables.SMTP_TO.getKey())))
                .thenReturn("recipient@server.cz");
        final DataValidator validator = new SimpleEmailServerValidator();
        final DataValidator.Status result = validator.validateData(data);
        assertThat(result).isEqualTo(DataValidator.Status.OK);
    }

    @Test
    void wrongPassword() {
        final InstallData data = mock(InstallData.class);
        when(data.getVariable(eq(Variables.EMAIL_CONFIGURATION_TYPE.getKey()))).thenReturn("seznam.cz");
        when(data.getVariable(eq(Variables.SMTP_AUTH.getKey()))).thenReturn("true");
        when(data.getVariable(eq(Variables.SMTP_IS_SSL.getKey()))).thenReturn("false");
        when(data.getVariable(eq(Variables.SMTP_IS_TLS.getKey()))).thenReturn("false");
        when(data.getVariable(eq(Variables.SMTP_USERNAME.getKey()))).thenReturn(USERNAME);
        when(data.getVariable(eq(Variables.SMTP_PASSWORD.getKey()))).thenReturn("");
        when(data.getVariable(eq(Variables.SMTP_PORT.getKey())))
                .thenReturn(String.valueOf(EMAIL.getSmtp().getPort()));
        when(data.getVariable(eq(Variables.SMTP_HOSTNAME.getKey())))
                .thenReturn(String.valueOf(EMAIL.getSmtp().getBindTo()));
        final DataValidator validator = new SimpleEmailServerValidator();
        final DataValidator.Status result = validator.validateData(data);
        assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }

}
