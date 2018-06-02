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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.github.robozonky.common.secrets.KeyStoreHandler;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.installer.scripts.RunScriptGenerator;
import com.github.robozonky.installer.scripts.ServiceGenerator;
import com.github.robozonky.internal.api.Settings;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Pack;
import com.izforge.izpack.api.event.AbstractInstallerListener;
import com.izforge.izpack.api.event.ProgressListener;
import org.apache.commons.lang3.SystemUtils;

public final class RoboZonkyInstallerListener extends AbstractInstallerListener {

    final static char[] KEYSTORE_PASSWORD = UUID.randomUUID().toString().toCharArray();
    private static final Logger LOGGER = Logger.getLogger(RoboZonkyInstallerListener.class.getSimpleName());
    static File INSTALL_PATH, DIST_PATH, KEYSTORE_FILE, JMX_PROPERTIES_FILE, EMAIL_CONFIG_FILE, SETTINGS_FILE,
            CLI_CONFIG_FILE, LOGBACK_CONFIG_FILE;
    private static InstallData DATA;
    private OS operatingSystem = OS.OTHER;

    public RoboZonkyInstallerListener() {
        if (SystemUtils.IS_OS_LINUX) {
            operatingSystem = OS.LINUX;
        } else if (SystemUtils.IS_OS_WINDOWS) {
            operatingSystem = OS.WINDOWS;
        }
    }

    /**
     * Testing OS-specific behavior was proving very difficult, this constructor takes all of that pain away.
     * @param os Fake operating system used for testing.
     */
    RoboZonkyInstallerListener(final OS os) {
        operatingSystem = os;
    }

    /**
     * This is a dirty ugly hack to workaround a bug in IZPack's Picocontainer. If we had the proper constructor to
     * accept {@link InstallData}, Picocontainer would have thrown some weird exception.
     * <p>
     * Therefore we share the data this way - through the last panel before the actual installation starts.
     * <p>
     * See more at https://izpack.atlassian.net/browse/IZPACK-1403.
     * @param data Installer data to store.
     */
    static void setInstallData(final InstallData data) {
        RoboZonkyInstallerListener.DATA = data;
        INSTALL_PATH = new File(Variables.INSTALL_PATH.getValue(DATA));
        DIST_PATH = new File(INSTALL_PATH, "Dist/");
        KEYSTORE_FILE = new File(INSTALL_PATH, "robozonky.keystore");
        JMX_PROPERTIES_FILE = new File(INSTALL_PATH, "management.properties");
        EMAIL_CONFIG_FILE = new File(INSTALL_PATH, "robozonky-notifications.cfg");
        SETTINGS_FILE = new File(INSTALL_PATH, "robozonky.properties");
        CLI_CONFIG_FILE = new File(INSTALL_PATH, "robozonky.cli");
        LOGBACK_CONFIG_FILE = new File(INSTALL_PATH, "logback.xml");
    }

    static void resetInstallData() {
        RoboZonkyInstallerListener.DATA = null;
        INSTALL_PATH = null;
        DIST_PATH = null;
        KEYSTORE_FILE = null;
        JMX_PROPERTIES_FILE = null;
        EMAIL_CONFIG_FILE = null;
        SETTINGS_FILE = null;
        CLI_CONFIG_FILE = null;
        LOGBACK_CONFIG_FILE = null;
    }

    CommandLinePart prepareStrategy() {
        final String content = Variables.STRATEGY_SOURCE.getValue(DATA);
        if (Objects.equals(Variables.STRATEGY_TYPE.getValue(DATA), "file")) {
            final File strategyFile = new File(INSTALL_PATH, "robozonky-strategy.cfg");
            try {
                Util.copyFile(new File(content), strategyFile);
                return new CommandLinePart().setOption("-s", strategyFile.getAbsolutePath());
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
            return new CommandLinePart().setOption("-i", EMAIL_CONFIG_FILE.toURI().toURL().toExternalForm());
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed writing e-mail configuration.", ex);
        }
    }

    private SecretProvider getSecretProvider(final char[] keystorePassword) {
        final String username = Variables.ZONKY_USERNAME.getValue(DATA);
        final char[] password = Variables.ZONKY_PASSWORD.getValue(DATA).toCharArray();
        try {
            KEYSTORE_FILE.delete();
            final KeyStoreHandler keystore = KeyStoreHandler.create(KEYSTORE_FILE, keystorePassword);
            return SecretProvider.keyStoreBased(keystore, username, password);
        } catch (final Exception ex) {
            RoboZonkyInstallerListener.LOGGER.log(Level.INFO, "Not creating guarded storage.", ex);
            return SecretProvider.fallback(username, password);
        }
    }

    CommandLinePart prepareJmx() {
        final boolean isJmxEnabled = Boolean.valueOf(Variables.IS_JMX_ENABLED.getValue(DATA));
        final CommandLinePart clp = new CommandLinePart()
                .setProperty("com.sun.management.jmxremote", isJmxEnabled ? "true" : "false")
                // the buffer is effectively a memory leak; we'll reduce its size from 1000 to 10
                .setProperty("jmx.remote.x.notification.buffer.size", "10");
        if (!isJmxEnabled) { // ignore JMX
            return clp;
        }
        // write JMX properties file
        final Properties props = new Properties();
        props.setProperty("com.sun.management.jmxremote.authenticate",
                          Variables.IS_JMX_SECURITY_ENABLED.getValue(DATA));
        props.setProperty("com.sun.management.jmxremote.ssl", "false");
        final String port = Variables.JMX_PORT.getValue(DATA);
        props.setProperty("com.sun.management.jmxremote.rmi.port", port);
        props.setProperty("com.sun.management.jmxremote.port", port);
        try {
            Util.writeOutProperties(props, JMX_PROPERTIES_FILE);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed writing JMX configuration.", ex);
        }
        // configure JMX to read the props file
        return clp.setProperty("com.sun.management.config.file", JMX_PROPERTIES_FILE.getAbsolutePath())
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

    private File assembleCliFile(final CommandLinePart credentials, final CommandLinePart strategy) throws IOException {
        // assemble the CLI
        final CommandLinePart cli = new CommandLinePart();
        Util.copyOptions(credentials, cli);
        cli.setOption("daemon");
        Util.copyOptions(strategy, cli);
        // store it to a file
        cli.storeOptions(CLI_CONFIG_FILE);
        return CLI_CONFIG_FILE.getAbsoluteFile();
    }

    CommandLinePart prepareCommandLine(final CommandLinePart strategy, final CommandLinePart emailConfig,
                                       final CommandLinePart jmxConfig, final CommandLinePart credentials,
                                       final CommandLinePart logging) {
        try {
            final File cliConfigFile = assembleCliFile(credentials, strategy);
            // have the CLI file loaded during RoboZonky startup
            final CommandLinePart commandLine = new CommandLinePart()
                    .setOption("@" + cliConfigFile.getAbsolutePath())
                    .setProperty(Settings.FILE_LOCATION_PROPERTY, SETTINGS_FILE.getAbsolutePath())
                    .setEnvironmentVariable("JAVA_HOME", "");
            // now proceed to set all system properties and settings
            final Properties settings = new Properties();
            Stream.of(strategy, emailConfig, jmxConfig, credentials, logging)
                    .map(CommandLinePart::getProperties)
                    .flatMap(p -> p.entrySet().stream())
                    .forEach(e -> {
                        final String key = e.getKey();
                        final String value = e.getValue();
                        if (key.startsWith("robozonky")) { // RoboZonky setting to be written to separate file
                            settings.setProperty(key, value);
                        } else { // general Java system property to end up on the command line
                            commandLine.setProperty(key, value);
                        }
                    });
            // write settings to a file
            Util.writeOutProperties(settings, SETTINGS_FILE);
            return commandLine;
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed writing CLI.", ex);
        }
    }

    void prepareLinuxServices(final File runScript) {
        for (final ServiceGenerator serviceGenerator : ServiceGenerator.values()) {
            final File result = serviceGenerator.apply(runScript);
            LOGGER.info("Generated " + result + " as a " + serviceGenerator + " service.");
        }
    }

    void prepareRunScript(final CommandLinePart commandLine) {
        if (System.getProperty("java.version").startsWith("1.8")) { // use G1GC on Java 8
            commandLine.setJvmArgument("XX:+UseG1GC");
        }
        commandLine.setJvmArgument("Xmx32m");
        final RunScriptGenerator generator = operatingSystem == OS.WINDOWS ?
                RunScriptGenerator.forWindows(DIST_PATH, CLI_CONFIG_FILE)
                : RunScriptGenerator.forUnix(DIST_PATH, CLI_CONFIG_FILE);
        final File runScript = generator.apply(commandLine);
        final File distRunScript = generator.getChildRunScript();
        Stream.of(runScript, distRunScript).forEach(script -> {
            LOGGER.info("Making executable: " + script);
            script.setExecutable(true);
        });
        if (operatingSystem == OS.LINUX) {
            prepareLinuxServices(runScript);
        }
    }

    CommandLinePart prepareLogging() {
        try {
            Util.copyFile(new File(DIST_PATH, "logback.xml"), LOGBACK_CONFIG_FILE);
            return new CommandLinePart()
                    .setProperty("logback.configurationFile", LOGBACK_CONFIG_FILE.getAbsolutePath());
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed copying log file.", ex);
        }
    }

    @Override
    public void afterPacks(final List<Pack> packs, final ProgressListener progressListener) {
        try {
            progressListener.startAction("Konfigurace RoboZonky", 7);
            progressListener.nextStep("Příprava strategie.", 1, 1);
            final CommandLinePart strategyConfig = prepareStrategy();
            progressListener.nextStep("Příprava nastavení e-mailu.", 2, 1);
            final CommandLinePart emailConfig = prepareEmailConfiguration();
            progressListener.nextStep("Příprava nastavení JMX.", 3, 1);
            final CommandLinePart jmx = prepareJmx();
            progressListener.nextStep("Příprava nastavení Zonky.", 4, 1);
            final CommandLinePart credentials = prepareCore();
            progressListener.nextStep("Příprava nastavení logování.", 5, 1);
            final CommandLinePart logging = prepareLogging();
            progressListener.nextStep("Generování parametrů příkazové řádky.", 6, 1);
            final CommandLinePart result = prepareCommandLine(strategyConfig, emailConfig, jmx, credentials, logging);
            progressListener.nextStep("Generování spustitelného souboru.", 7, 1);
            prepareRunScript(result);
            progressListener.stopAction();
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Uncaught exception.", ex);
            throw new IllegalStateException("Uncaught exception.", ex);
        }
    }

    enum OS {
        WINDOWS,
        LINUX,
        OTHER
    }
}
