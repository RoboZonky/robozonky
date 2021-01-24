/*
 * Copyright 2021 The RoboZonky Project
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

import java.util.Properties;

import com.izforge.izpack.api.data.InstallData;

final class Util {

    private Util() {
        // no instances
    }

    static String toBoolean(final String string) {
        return Boolean.valueOf(string)
            .toString();
    }

    static String toInt(final String string) {
        if (string == null) {
            return "-1";
        }
        return String.valueOf(Integer.parseInt(string));
    }

    public static Properties configureEmailNotifications(final InstallData data) {
        final Properties p = new Properties();
        p.setProperty("email.enabled", "true");
        p.setProperty("email.to", Variables.SMTP_TO.getValue(data));
        p.setProperty("email.from", Variables.SMTP_USERNAME.getValue(data)); // seznam.cz demands, gmail does it anyway
        p.setProperty("email.smtp.requiresAuthentication", toBoolean(Variables.SMTP_AUTH.getValue(data)));
        p.setProperty("email.smtp.username", Variables.SMTP_USERNAME.getValue(data));
        p.setProperty("email.smtp.password", Variables.SMTP_PASSWORD.getValue(data));
        p.setProperty("email.smtp.hostname", Variables.SMTP_HOSTNAME.getValue(data));
        p.setProperty("email.smtp.port", toInt(Variables.SMTP_PORT.getValue(data)));
        p.setProperty("email.smtp.requiresStartTLS", toBoolean(Variables.SMTP_IS_TLS.getValue(data)));
        p.setProperty("email.smtp.requiresSslOnConnect", toBoolean(Variables.SMTP_IS_SSL.getValue(data)));
        final String isInvestmentEmailEnabled = toBoolean(Variables.EMAIL_IS_INVESTMENT.getValue(data));
        p.setProperty("email.investmentPurchased.enabled", isInvestmentEmailEnabled);
        p.setProperty("email.investmentMade.enabled", isInvestmentEmailEnabled);
        p.setProperty("email.investmentSold.enabled", isInvestmentEmailEnabled);
        p.setProperty("email.reservationAccepted.enabled", isInvestmentEmailEnabled);
        p.setProperty("email.loanLost.enabled", isInvestmentEmailEnabled);
        p.setProperty("email.loanDefaulted.enabled", isInvestmentEmailEnabled);
        p.setProperty("email.roboZonkyCrashed.enabled", "true");
        p.setProperty("email.roboZonkyCrashed.hourlyMaxEmails", "-1");
        final String isFailureEmailEnabled = toBoolean(Variables.EMAIL_IS_FAILURE.getValue(data));
        p.setProperty("email.roboZonkyDaemonSuspended.enabled", isFailureEmailEnabled);
        p.setProperty("email.roboZonkyDaemonSuspended.hourlyMaxEmails", "20");
        p.setProperty("email.roboZonkyDaemonResumed.enabled", isFailureEmailEnabled);
        p.setProperty("email.roboZonkyDaemonResumed.hourlyMaxEmails", "20");
        p.setProperty("email.weeklySummary.enabled", toBoolean(Variables.EMAIL_IS_WEEKLY.getValue(data)));
        p.setProperty("email.roboZonkyUpdateDetected.enabled", "true");
        p.setProperty("email.roboZonkyUpdateDetected.hourlyMaxEmails", "1");
        return p;
    }

}
