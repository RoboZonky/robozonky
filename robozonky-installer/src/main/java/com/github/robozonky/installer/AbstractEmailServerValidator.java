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

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;

abstract class AbstractEmailServerValidator extends AbstractValidator {

    private ClosingTransport transport;

    private static Properties getSmtpProperties(final InstallData installData) {
        final Properties props = new Properties();
        props.setProperty("mail.smtp.auth", Variables.SMTP_AUTH.getValue(installData));
        props.setProperty("mail.smtp.starttls.enable", Variables.SMTP_IS_TLS.getValue(installData));
        props.setProperty("mail.smtp.ssl.enable", Variables.SMTP_IS_SSL.getValue(installData));
        return props;
    }

    /**
     * The whole point of this horrible hack is to give the tests a chance to catch the try-with-resources statement
     * happening in {@link #validateDataPossiblyThrowingException(InstallData)}. Otherwise PITest would show failing
     * mutations that are unkillable.
     * @return
     */
    ClosingTransport getTransport() {
        return transport;
    }

    @Override
    protected Status validateDataPossiblyThrowingException(final InstallData installData) {
        configure(installData);
        final Properties smtpProps = getSmtpProperties(installData);
        final Session session = Session.getInstance(smtpProps, null);
        try (final ClosingTransport t = new ClosingTransport(session.getTransport("smtp"))) {
            transport = t;
            final String host = Variables.SMTP_HOSTNAME.getValue(installData);
            final int port = Integer.parseInt(Variables.SMTP_PORT.getValue(installData));
            LOGGER.debug("Connecting to {}:{} with {}.", host, port, smtpProps);
            t.getTransport().connect(host, port, Variables.SMTP_USERNAME.getValue(installData),
                                     Variables.SMTP_PASSWORD.getValue(installData));
            return DataValidator.Status.OK;
        } catch (final MessagingException ex) {
            LOGGER.warn("Failed authenticating with SMTP server.", ex);
            return DataValidator.Status.WARNING;
        }
    }

    abstract void configure(final InstallData data);

    @Override
    public String getWarningMessageId() {
        return "Nepodařilo se přihlásit k e-mailovému serveru. E-mailové notifikace pravděpodobně nebudou fungovat.";
    }

    @Override
    public String getErrorMessageId() {
        return "Něco se nepodařilo. Pravděpodobně se jedná o chybu v RoboZonky.";
    }

    static final class ClosingTransport implements AutoCloseable {

        private final Transport transport;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        public ClosingTransport(final Transport transport) {
            this.transport = transport;
        }

        public Transport getTransport() {
            return transport;
        }

        public AtomicBoolean getClosed() {
            return closed;
        }

        @Override
        public void close() throws MessagingException {
            closed.set(true);
            transport.close();
        }
    }
}
