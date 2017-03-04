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

package com.github.triceo.robozonky.installer.panels;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

public class EmailSettingsValidator implements DataValidator {

    private static final Logger LOGGER = Logger.getLogger(EmailSettingsValidator.class.getCanonicalName());

    @Override
    public DataValidator.Status validateData(final InstallData installData) {
        final Email email = new SimpleEmail();
        email.setSubject("RoboZonky: Správně nastavené e-mailové notifikace");
        email.setHostName(Variables.SMTP_HOSTNAME.getValue(installData));
        email.setSmtpPort(Integer.parseInt(Variables.SMTP_PORT.getValue(installData)));
        email.setStartTLSRequired(Boolean.valueOf(Variables.SMTP_IS_TLS.getValue(installData)));
        email.setSSLOnConnect(Boolean.valueOf(Variables.SMTP_IS_SSL.getValue(installData)));
        final String username = Variables.SMTP_USERNAME.getValue(installData);
        email.setAuthentication(username, Variables.SMTP_PASSWORD.getValue(installData));
        try {
            email.setMsg("Tento e-mail je důkazem, že jste správně nastavili e-mailové notifikace. Gratulujeme!");
            email.setFrom(username, "RoboZonky instalátor");
            email.addTo(Variables.SMTP_TO.getValue(installData));
            email.send();
            return DataValidator.Status.OK;
        } catch (final Exception ex) {
            EmailSettingsValidator.LOGGER.log(Level.WARNING, "Failed sending e-mail.", ex);
            return DataValidator.Status.WARNING;
        }
    }

    @Override
    public String getErrorMessageId() {
        return null;
    }

    @Override
    public String getWarningMessageId() {
        return "Došlo k chybě při komunikaci s SMTP " +
                "E-mailové notifikace nemusí fungovat správně.";
    }

    @Override
    public boolean getDefaultAnswer() {
        return false;
    }
}
