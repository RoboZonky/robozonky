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

package com.github.triceo.robozonky.installer.panels;

import java.util.UUID;
import javax.mail.MessagingException;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class EmailSettingsValidatorTest {

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    @Test
    public void messages() {
        final DataValidator validator = new EmailSettingsValidator();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(validator.getDefaultAnswer()).isFalse();
            softly.assertThat(validator.getWarningMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEqualTo(validator.getWarningMessageId());
        });
    }

    @Test
    public void mailSent() throws MessagingException, InterruptedException {
        final InstallData data = Mockito.mock(InstallData.class);
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.ZONKY_USERNAME.getKey())))
                .thenReturn("someone@somewhere.cz");
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.SMTP_PORT.getKey())))
                .thenReturn(String.valueOf(greenMail.getSmtp().getPort()));
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.SMTP_HOSTNAME.getKey())))
                .thenReturn(String.valueOf(greenMail.getSmtp().getBindTo()));
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.SMTP_TO.getKey())))
                .thenReturn("recipient@server.cz");
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.SMTP_USERNAME.getKey())))
                .thenReturn("sender@server.cz");
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.SMTP_PASSWORD.getKey())))
                .thenReturn(UUID.randomUUID().toString());
        final DataValidator validator = new EmailSettingsValidator();
        final DataValidator.Status result = validator.validateData(data);
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.OK);
    }

    @Test
    public void mailFailed() throws MessagingException {
        final InstallData data = Mockito.mock(InstallData.class);
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.SMTP_PORT.getKey())))
                .thenReturn(String.valueOf(greenMail.getSmtp().getPort()));
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.SMTP_HOSTNAME.getKey())))
                .thenReturn(String.valueOf(greenMail.getSmtp().getBindTo()));
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.SMTP_USERNAME.getKey())))
                .thenReturn("sender@server.cz");
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.SMTP_PASSWORD.getKey())))
                .thenReturn(UUID.randomUUID().toString());
        final DataValidator validator = new EmailSettingsValidator();
        final DataValidator.Status result = validator.validateData(data);
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.WARNING);
        Assertions.assertThat(greenMail.getReceivedMessages()).hasSize(0);
    }
}
