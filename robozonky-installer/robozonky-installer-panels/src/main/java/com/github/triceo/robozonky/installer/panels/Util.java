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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;

import com.github.triceo.robozonky.internal.api.Defaults;
import com.izforge.izpack.api.data.InstallData;

class Util {

    private static final String toBoolean(final String string) {
        return Boolean.valueOf(string).toString();
    }

    private static final String toInt(final String string) {
        if (string == null) {
            return "-1";
        }
        return String.valueOf(Integer.parseInt(string));
    }

    public static void writeOutProperties(final Properties properties, final File target) throws IOException {
        try (final Writer w = new OutputStreamWriter(new FileOutputStream(target), Defaults.CHARSET)) {
            properties.store(w, "Konfigurace e-mailových notifikací z RoboZonky");
        }
    }

    public static Properties configureEmailNotifications(final InstallData data) {
        final Properties p = new Properties();
        p.setProperty("enabled", "true");
        p.setProperty("to", Variables.SMTP_TO.getValue(data));
        p.setProperty("smtp.username", Variables.SMTP_USERNAME.getValue(data));
        p.setProperty("smtp.password", Variables.SMTP_PASSWORD.getValue(data));
        p.setProperty("smtp.hostname", Variables.SMTP_HOSTNAME.getValue(data));
        p.setProperty("smtp.port", toInt(Variables.SMTP_PORT.getValue(data)));
        p.setProperty("smtp.requiresStartTLS", toBoolean(Variables.SMTP_IS_TLS.getValue(data)));
        p.setProperty("smtp.requiresSslOnConnect", toBoolean(Variables.SMTP_IS_SSL.getValue(data)));
        final String isInvestmentEmailEnabled = toBoolean(Variables.EMAIL_IS_INVESTMENT.getValue(data));
        p.setProperty("investmentSkipped.enabled", isInvestmentEmailEnabled);
        p.setProperty("investmentRejected.enabled", isInvestmentEmailEnabled);
        p.setProperty("investmentMade.enabled", isInvestmentEmailEnabled);
        p.setProperty("investmentDelegated.enabled", isInvestmentEmailEnabled);
        p.setProperty("balanceTracker.enabled", toBoolean(Variables.EMAIL_IS_BALANCE_OVER_200.getValue(data)));
        p.setProperty("balanceTracker.targetBalance", "200");
        p.setProperty("roboZonkyDaemonFailed.enabled", toBoolean(Variables.EMAIL_IS_FAILURE.getValue(data)));
        p.setProperty("roboZonkyCrashed.enabled", toBoolean(Variables.EMAIL_IS_CRITICAL_FAILURE.getValue(data)));
        p.setProperty("hourlyMaxEmails", "20");
        return p;
    }

}
