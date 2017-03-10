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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.common.secrets.KeyStoreHandler;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
import com.github.triceo.robozonky.notifications.email.EmailListenerService;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Pack;
import com.izforge.izpack.api.event.AbstractInstallerListener;
import com.izforge.izpack.api.event.ProgressListener;

public final class RoboZonkyInstallerListener extends AbstractInstallerListener {

    private static final Logger LOGGER = Logger.getLogger(RoboZonkyInstallerListener.class.getSimpleName());
    private static InstallData DATA;
    final static char[] KEYSTORE_PASSWORD = UUID.randomUUID().toString().toCharArray();
    static File INSTALL_PATH, DIST_PATH, KEYSTORE_FILE, EMAIL_CONFIG_FILE, CLI_CONFIG_FILE;

    /**
     * This is a dirty ugly hack to workaround a bug in IZPack's Picocontainer. If we had the proper constructor to
     * accept {@link InstallData}, Picocontainer would have thrown some weird exception.
     *
     * Therefore we share the data this way - through the last panel before the actual installation starts.
     *
     * See more at https://izpack.atlassian.net/browse/IZPACK-1403.
     *
     * @param data Installer data to store.
     */
    static void setInstallData(final InstallData data) {
        RoboZonkyInstallerListener.DATA = data;
        INSTALL_PATH = new File(Variables.INSTALL_PATH.getValue(DATA));
        DIST_PATH = new File(INSTALL_PATH, "Dist/");
        KEYSTORE_FILE = new File(INSTALL_PATH, "robozonky.keystore");
        EMAIL_CONFIG_FILE = new File(INSTALL_PATH, "robozonky-notifications.cfg");
        CLI_CONFIG_FILE = new File(INSTALL_PATH, "robozonky.cli");
    }

    static void resetInstallData() {
        RoboZonkyInstallerListener.DATA = null;
        INSTALL_PATH = null;
        DIST_PATH = null;
        KEYSTORE_FILE = null;
        EMAIL_CONFIG_FILE = null;
        CLI_CONFIG_FILE = null;
    }

    CommandLinePart prepareStrategy() {
        final String content = Variables.STRATEGY_SOURCE.getValue(DATA);
        if (Objects.equals(Variables.STRATEGY_TYPE.getValue(DATA), "file")) {
            final File strategyFile = new File(INSTALL_PATH, "robozonky-strategy.cfg");
            try {
                Files.copy(new File(content).toPath(), strategyFile.toPath());
                return new CommandLinePart().setOption("-s", strategyFile.getName());
            } catch (final IOException ex) {
                throw new IllegalStateException("Failed copying strategy file.", ex);
            }
        } else {
            try {
                return new CommandLinePart().setOption("-s", new URL(content).toExternalForm());
            } catch (final MalformedURLException ex) {
                throw new IllegalStateException("Wrong strategy URL.", ex);
            }
        }
    }

    CommandLinePart prepareEmailConfiguration() {
        if (!Boolean.valueOf(Variables.IS_EMAIL_ENABLED.getValue(DATA))) {
            return new CommandLinePart();
        }
        final Properties p = Util.configureEmailNotifications(DATA);
        try {
            Util.writeOutProperties(p, EMAIL_CONFIG_FILE);
            return new CommandLinePart().setProperty(EmailListenerService.CONFIG_FILE_LOCATION_PROPERTY,
                    EMAIL_CONFIG_FILE.toURI().toURL().toExternalForm());
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed writing e-mail configuration.", ex);
        }
    }

    private SecretProvider getSecretProvider(final char[] keystorePassword) {
        final String username = Variables.ZONKY_USERNAME.getValue(DATA);
        final char[] password = Variables.ZONKY_PASSWORD.getValue(DATA).toCharArray();
        try {
            final KeyStoreHandler keystore = KeyStoreHandler.create(KEYSTORE_FILE, keystorePassword);
            return SecretProvider.keyStoreBased(keystore, username, password);
        } catch (final Exception ex) {
            RoboZonkyInstallerListener.LOGGER.log(Level.INFO, "Not creating guarded storage.", ex);
            return SecretProvider.fallback(username, password);
        }
    }

    CommandLinePart prepareJmx() {
        if (!Boolean.valueOf(Variables.IS_JMX_ENABLED.getValue(DATA))) {
            return new CommandLinePart();
        }
        final String port = Variables.JMX_PORT.getValue(DATA);
        return new CommandLinePart()
                .setProperty("com.sun.management.jmxremote.authenticate",
                        Variables.IS_JMX_SECURITY_ENABLED.getValue(DATA))
                .setProperty("com.sun.management.jmxremote.ssl", "false")
                .setProperty("com.sun.management.jmxremote.rmi.port", port)
                .setProperty("com.sun.management.jmxremote.port", port)
                .setProperty("java.rmi.server.hostname", Variables.JMX_HOSTNAME.getValue(DATA));
    }

    CommandLinePart prepareCore() {
        return prepareCore(KEYSTORE_PASSWORD);
    }

    CommandLinePart prepareCore(final char[] keystorePassword) {
        final SecretProvider secrets = getSecretProvider(keystorePassword);
        final String zonkoidId = "zonkoid";
        final CommandLinePart cli = new CommandLinePart()
                .setOption("-p", String.valueOf(secrets.isPersistent() ? KEYSTORE_PASSWORD : secrets.getPassword()));
        if (Boolean.valueOf(Variables.IS_DRY_RUN.getValue(DATA))) {
            cli.setOption("-d");
        }
        if (Boolean.valueOf(Variables.IS_USING_OAUTH_TOKEN.getValue(DATA))) {
            cli.setOption("-r");
        }
        final boolean isZonkoidEnabled = Boolean.valueOf(Variables.IS_ZONKOID_ENABLED.getValue(DATA));
        if (secrets.isPersistent() && KEYSTORE_FILE.canRead()) {
            cli.setOption("-g", KEYSTORE_FILE.getAbsolutePath());
            if (isZonkoidEnabled) {
                cli.setOption("-x", zonkoidId);
                secrets.setSecret(zonkoidId, Variables.ZONKOID_TOKEN.getValue(DATA).toCharArray());
            }
        } else {
            cli.setOption("-u", secrets.getUsername());
            if (isZonkoidEnabled) {
                cli.setOption("-x", zonkoidId + ":" + Variables.ZONKOID_TOKEN.getValue(DATA));
            }
        }
        return cli;
    }

    CommandLinePart prepareCommandLine(final CommandLinePart strategy, final CommandLinePart emailConfig,
                                       final CommandLinePart jmxConfig, final CommandLinePart credentials) {
        // assemble the CLI
        final CommandLinePart cli = new CommandLinePart();
        credentials.getOptions().forEach((k, v) -> cli.setOption(k, v.toArray(new String[v.size()])));
        cli.setOption("daemon");
        strategy.getOptions().forEach((k, v) -> cli.setOption(k, v.toArray(new String[v.size()])));
        try {
            // store it to a file
            cli.storeOptions(CLI_CONFIG_FILE);
            // and have the file loaded during RoboZonky startup
            final CommandLinePart result = new CommandLinePart()
                    .setOption("@" + CLI_CONFIG_FILE.getAbsolutePath())
                    .setEnvironmentVariable("JAVA_HOME", Variables.JAVA_HOME.getValue(DATA));
            Stream.of(strategy.getProperties(), emailConfig.getProperties(), jmxConfig.getProperties(),
                    credentials.getProperties())
                    .forEach(m -> m.forEach(result::setProperty));
            return result;
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed writing CLI.", ex);
        }
    }

    private Collection<String> getScript(final CommandLinePart commandLine,
                                         final BiFunction<String, String, String> envConverter,
                                         final String javaOptsPrefix) {
        final Collection<String> result = new ArrayList<>();
        commandLine.getEnvironmentVariables().forEach((k, v) -> result.add(envConverter.apply(k, v)));
        final String javaOpts = commandLine.getProperties().entrySet().stream()
                .map(e -> "-D" + e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(" ", javaOptsPrefix, "\""));
        result.add(javaOpts);
        return result;
    }

    private String createScript(final CommandLinePart commandLine, final String name) {
        final File subexecutable = new File(DIST_PATH, name);
        subexecutable.setExecutable(true);
        return subexecutable.getPath() + " " + commandLine.convertOptions();
    }

    private Collection<String> getWindowsScript(final CommandLinePart commandLine) {
        final Collection<String> result =
                this.getScript(commandLine, (s, s2) -> "set \"" + s + "=" + s2 + "\"", "set \"JAVA_OPTS=%JAVA_OPTS% ");
        result.add(this.createScript(commandLine, "robozonky.bat"));
        return result;
    }

    private Collection<String> getUnixScript(final CommandLinePart commandLine) {
        final Collection<String> result = new ArrayList<>();
        result.add("#!/bin/bash");
        result.addAll(this.getScript(commandLine, (s, s2) -> s + "=\"" + s2 + "\"",
                "JAVA_OPTS=\"$JAVA_OPTS "));
        // make script executable
        result.add(this.createScript(commandLine, "robozonky.sh"));
        return result;
    }

    void prepareRunScript(final CommandLinePart commandLine) {
        final boolean isWindows = Boolean.valueOf(Variables.IS_WINDOWS.getValue(DATA));
        final Collection<String> lines = isWindows ? getWindowsScript(commandLine) : getUnixScript(commandLine);
        try {
            final File file = new File(INSTALL_PATH, isWindows ? "run.bat" : "run.sh");
            Files.write(file.toPath(), lines, Charset.defaultCharset());
            file.setExecutable(true);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed writing executable.", ex);
        }
    }

    void moveLog() {
        final File logFile = new File(DIST_PATH, "logback.xml");
        final File targetFile = new File(INSTALL_PATH, "logback.xml");
        try {
            Files.copy(logFile.toPath(), targetFile.toPath());
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed copying log file.", ex);
        }
    }

    @Override
    public void afterPacks(final List<Pack> packs, final ProgressListener progressListener) {
        progressListener.startAction("Konfigurace RoboZonky", 7);
        progressListener.nextStep("Příprava strategie.", 1, 1);
        final CommandLinePart strategyConfig = prepareStrategy();
        progressListener.nextStep("Příprava nastavení e-mailu.", 2, 1);
        final CommandLinePart emailConfig = prepareEmailConfiguration();
        progressListener.nextStep("Příprava nastavení JMX.", 3, 1);
        final CommandLinePart jmx = prepareJmx();
        progressListener.nextStep("Příprava nastavení Zonky.", 4, 1);
        final CommandLinePart credentials = prepareCore();
        progressListener.nextStep("Generování parametrů příkazové řádky.", 5, 1);
        final CommandLinePart result = prepareCommandLine(strategyConfig, emailConfig, jmx, credentials);
        progressListener.nextStep("Příprava nastavení logování.", 6, 1);
        moveLog();
        progressListener.nextStep("Generování spustitelného souboru.", 7, 1);
        prepareRunScript(result);
        progressListener.stopAction();
    }
}
