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

package com.github.robozonky.installer;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;

import com.github.robozonky.common.extensions.Checker;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;

public class EmailSettingsValidator extends AbstractValidator {

    @Override
    protected DataValidator.Status validateDataPossiblyThrowingException(final InstallData installData) {
        try {
            // configure e-mail notification properties
            final Properties emailConfig = Util.configureEmailNotifications(installData);
            final File emailConfigTarget = File.createTempFile("robozonky-", ".cfg");
            Util.writeOutProperties(emailConfig, emailConfigTarget);
            return Checker.notifications(Variables.ZONKY_USERNAME.getValue(installData),
                                         emailConfigTarget.toURI().toURL())
                    ? DataValidator.Status.OK : DataValidator.Status.ERROR;
        } catch (final Exception ex) {
            LOGGER.log(Level.WARNING, "Failed sending e-mail.", ex);
            return DataValidator.Status.WARNING;
        }
    }

    @Override
    public String getErrorMessageId() {
        return "Konfigurace se nezdařila. Pravděpodobně se jedná o chybu v RoboZonky.";
    }

    @Override
    public String getWarningMessageId() {
        return "Došlo k chybě při komunikaci s SMTP. E-mailové notifikace nemusí fungovat správně.";
    }
}
