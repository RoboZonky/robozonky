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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.util.IoUtil;
import com.izforge.izpack.api.data.InstallData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Util {

    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private Util() {
        // no instances
    }

    private static String toBoolean(final String string) {
        return Boolean.valueOf(string).toString();
    }

    private static String toInt(final String string) {
        if (string == null) {
            return "-1";
        }
        return String.valueOf(Integer.parseInt(string));
    }

    public static void writeOutProperties(final Properties properties, final File target) throws IOException {
        IoUtil.tryConsumer(() -> Files.newBufferedWriter(target.toPath(), Defaults.CHARSET),
                               w -> properties.store(w, Defaults.ROBOZONKY_USER_AGENT));
        LOGGER.debug("Written properties to {}.", target);
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
        p.setProperty("email.saleOffered.enabled", isInvestmentEmailEnabled);
        p.setProperty("email.loanRepaid.enabled", isInvestmentEmailEnabled);
        p.setProperty("email.loanLost.enabled", isInvestmentEmailEnabled);
        p.setProperty("email.loanDefaulted.enabled", isInvestmentEmailEnabled);
        p.setProperty("email.loanNoLongerDelinquent.enabled", isInvestmentEmailEnabled);
        p.setProperty("email.balanceTracker.enabled", toBoolean(Variables.EMAIL_IS_BALANCE_OVER_200.getValue(data)));
        p.setProperty("email.balanceTracker.targetBalance", "200");
        p.setProperty("email.roboZonkyDaemonFailed.enabled", toBoolean(Variables.EMAIL_IS_FAILURE.getValue(data)));
        p.setProperty("email.roboZonkyCrashed.enabled", toBoolean(Variables.EMAIL_IS_CRITICAL_FAILURE.getValue(data)));
        p.setProperty("email.roboZonkyUpdateDetected.enabled", "true");
        p.setProperty("email.roboZonkyUpdateDetected.maxHourlyEmails", "1");
        p.setProperty("email.hourlyMaxEmails", "20");
        return p;
    }

    public static void copyFile(final File from, final File to) throws IOException {
        final Path f = from.getAbsoluteFile().toPath();
        final Path t = to.getAbsoluteFile().toPath();
        LOGGER.debug("Copying {} to {}", f, t);
        Files.copy(f, t, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copyOptions(final CommandLinePart source, final CommandLinePart target) {
        source.getOptions().forEach((k, v) -> target.setOption(k, v.toArray(new String[0])));
    }

    static void processCommandLine(final CommandLinePart commandLine, final Properties settings,
                                   final CommandLinePart... parts) {
        Stream.of(parts)
                .map(CommandLinePart::getProperties)
                .flatMap(p -> p.entrySet().stream())
                .peek(e -> LOGGER.trace("Processing property {}.", e))
                .filter(e -> Objects.nonNull(e.getValue())) // prevent NPEs coming from evil code
                .forEach(e -> {
                    final String key = e.getKey();
                    final String value = e.getValue();
                    if (key.startsWith("robozonky")) { // RoboZonky settings to be written to a separate file
                        LOGGER.debug("Storing property {}.", e);
                        settings.setProperty(key, value);
                    } else { // general Java system property to end up on the command line
                        LOGGER.debug("Setting property {}.", e);
                        commandLine.setProperty(key, value);
                    }
                });
    }
}
