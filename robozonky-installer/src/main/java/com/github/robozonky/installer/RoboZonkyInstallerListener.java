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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import com.github.robozonky.cli.Feature;
import com.github.robozonky.cli.GoogleCredentialsFeature;
import com.github.robozonky.cli.SetupFailedException;
import com.github.robozonky.cli.ZonkoidPasswordFeature;
import com.github.robozonky.cli.ZonkyPasswordFeature;
import com.github.robozonky.installer.scripts.RunScriptGenerator;
import com.github.robozonky.installer.scripts.ServiceGenerator;
import com.github.robozonky.internal.api.Settings;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Pack;
import com.izforge.izpack.api.event.AbstractInstallerListener;
import com.izforge.izpack.api.event.ProgressListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.robozonky.integrations.stonky.Properties.GOOGLE_CALLBACK_HOST;
import static com.github.robozonky.integrations.stonky.Properties.GOOGLE_CALLBACK_PORT;
import static com.github.robozonky.integrations.stonky.Properties.GOOGLE_LOCAL_FOLDER;

public final class RoboZonkyInstallerListener extends AbstractInstallerListener {

    static final char[] KEYSTORE_PASSWORD = UUID.randomUUID().toString().toCharArray();
    private static final Logger LOGGER = LogManager.getLogger(RoboZonkyInstallerListener.class);
    static File INSTALL_PATH, DIST_PATH, KEYSTORE_FILE, JMX_PROPERTIES_FILE, EMAIL_CONFIG_FILE, SETTINGS_FILE,
            CLI_CONFIG_FILE, LOGBACK_CONFIG_FILE;
    private static InstallData DATA;
    private RoboZonkyInstallerListener.OS operatingSystem = RoboZonkyInstallerListener.OS.OTHER;

    public RoboZonkyInstallerListener() {
        if (SystemUtils.IS_OS_LINUX) {
            operatingSystem = RoboZonkyInstallerListener.OS.LINUX;
        } else if (SystemUtils.IS_OS_WINDOWS) {
            operatingSystem = RoboZonkyInstallerListener.OS.WINDOWS;
        }
    }

    /**
     * Testing OS-specific behavior was proving very difficult, this constructor takes all of that pain away.
     * @param os Fake operating system used for testing.
     */
    RoboZonkyInstallerListener(final RoboZonkyInstallerListener.OS os) {
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

    private static void primeKeyStore(final char... keystorePassword) throws SetupFailedException, IOException {
        final String username = Variables.ZONKY_USERNAME.getValue(DATA);
        final char[] password = Variables.ZONKY_PASSWORD.getValue(DATA).toCharArray();
        Files.deleteIfExists(KEYSTORE_FILE.toPath()); // re-install into the same directory otherwise fails
        final Feature f = new ZonkyPasswordFeature(KEYSTORE_FILE, keystorePassword, username, password);
        f.setup();
    }

    private static void prepareZonkoid(final char... keystorePassword) throws SetupFailedException {
        final Feature f = new ZonkoidPasswordFeature(KEYSTORE_FILE, keystorePassword,
                                                     Variables.ZONKOID_TOKEN.getValue(DATA).toCharArray());
        f.setup();
    }

    private static CommandLinePart prepareCore(
            final char... keystorePassword) throws SetupFailedException, IOException {
        final String zonkoidId = "zonkoid";
        final CommandLinePart cli = new CommandLinePart()
                .setOption("-p", String.valueOf(keystorePassword));
        cli.setOption("-g", KEYSTORE_FILE.getAbsolutePath());
        if (Boolean.valueOf(Variables.IS_DRY_RUN.getValue(DATA))) {
            cli.setOption("-d");
            cli.setJvmArgument("Xmx128m"); // more memory for the JFR recording
            cli.setJvmArgument("XX:StartFlightRecording=disk=true,dumponexit=true,maxage=1d,path-to-gc-roots=true");
        } else {
            cli.setJvmArgument("Xmx64m");
        }
        primeKeyStore(keystorePassword);
        final boolean isZonkoidEnabled = Boolean.parseBoolean(Variables.IS_ZONKOID_ENABLED.getValue(DATA));
        if (isZonkoidEnabled) {
            prepareZonkoid(keystorePassword);
            cli.setOption("-x", zonkoidId);
        }
        return cli;
    }

    private static File assembleCliFile(final CommandLinePart... source) throws IOException {
        // assemble the CLI
        final CommandLinePart cli = new CommandLinePart();
        Stream.of(source).forEach(c -> Util.copyOptions(c, cli));
        // store it to a file
        cli.storeOptions(CLI_CONFIG_FILE);
        return CLI_CONFIG_FILE.getAbsoluteFile();
    }

    static CommandLinePart prepareStrategy() {
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

    private static URL getEmailConfiguration() throws IOException {
        final String type = Variables.EMAIL_CONFIGURATION_TYPE.getValue(DATA);
        LOGGER.debug("Configuring notifications: {}", type);
        switch (type) {
            case "file":
                final File f = new File(Variables.EMAIL_CONFIGURATION_SOURCE.getValue(DATA));
                Util.copyFile(f, EMAIL_CONFIG_FILE);
                return EMAIL_CONFIG_FILE.toURI().toURL();
            case "url":
                return new URL(Variables.EMAIL_CONFIGURATION_SOURCE.getValue(DATA));
            default:
                final Properties props = Util.configureEmailNotifications(DATA);
                Util.writeOutProperties(props, EMAIL_CONFIG_FILE);
                return EMAIL_CONFIG_FILE.toURI().toURL();
        }
    }

    static CommandLinePart prepareEmailConfiguration() {
        if (!Boolean.valueOf(Variables.IS_EMAIL_ENABLED.getValue(DATA))) {
            return new CommandLinePart();
        }
        try {
            final URL url = getEmailConfiguration();
            return new CommandLinePart().setOption("-i", url.toExternalForm());
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed writing e-mail configuration.", ex);
        }
    }

    static CommandLinePart prepareJmx() {
        final boolean isJmxEnabled = Boolean.parseBoolean(Variables.IS_JMX_ENABLED.getValue(DATA));
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
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed writing JMX configuration.", ex);
        }
        // configure JMX to read the props file
        return clp.setProperty("com.sun.management.config.file", JMX_PROPERTIES_FILE.getAbsolutePath())
                .setProperty("java.rmi.server.hostname", Variables.JMX_HOSTNAME.getValue(DATA));
    }

    static CommandLinePart prepareCore() throws SetupFailedException, IOException {
        return prepareCore(KEYSTORE_PASSWORD);
    }

    private static CommandLinePart prepareCommandLine(final CommandLinePart strategy, final CommandLinePart emailConfig,
                                                      final CommandLinePart stonky, final CommandLinePart jmxConfig,
                                                      final CommandLinePart core, final CommandLinePart logging) {
        try {
            final File cliConfigFile = assembleCliFile(core, strategy, emailConfig);
            // have the CLI file loaded during RoboZonky startup
            final CommandLinePart commandLine = new CommandLinePart()
                    .setOption("@" + cliConfigFile.getAbsolutePath())
                    .setProperty(Settings.FILE_LOCATION_PROPERTY, SETTINGS_FILE.getAbsolutePath())
                    .setEnvironmentVariable("JAVA_HOME", "");
            // now proceed to set all system properties and settings
            final Properties settings = new Properties();
            Util.processCommandLine(commandLine, settings, strategy, stonky, jmxConfig, core, logging);
            // write settings to a file
            Util.writeOutProperties(settings, SETTINGS_FILE);
            return commandLine;
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed writing CLI.", ex);
        }
    }

    private static void prepareLinuxServices(final File runScript) {
        for (final ServiceGenerator serviceGenerator : ServiceGenerator.values()) {
            final File result = serviceGenerator.apply(runScript);
            LOGGER.info("Generated {} as a {} service.", result, serviceGenerator);
        }
    }

    private static CommandLinePart prepareLogging() {
        try {
            Util.copyFile(new File(DIST_PATH, "logback.xml"), LOGBACK_CONFIG_FILE);
            return new CommandLinePart()
                    .setProperty("logback.configurationFile", LOGBACK_CONFIG_FILE.getAbsolutePath());
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed copying log file.", ex);
        }
    }

    private static CommandLinePart prepareStonky() {
        try {
            final String host = Variables.GOOGLE_CALLBACK_HOST.getValue(DATA);
            final String port = Variables.GOOGLE_CALLBACK_PORT.getValue(DATA);
            final CommandLinePart cli = new CommandLinePart();
            if (Boolean.valueOf(Variables.IS_STONKY_ENABLED.getValue(DATA))) {
                cli.setProperty(GOOGLE_CALLBACK_HOST.getKey(), host);
                cli.setProperty(GOOGLE_CALLBACK_PORT.getKey(), port);
                // reuse the same code for this as we do in CLI
                LOGGER.debug("Preparing Google credentials.");
                final String username = Variables.ZONKY_USERNAME.getValue(DATA);
                final GoogleCredentialsFeature google = new GoogleCredentialsFeature(username);
                google.setHost(host);
                google.setPort(Integer.parseInt(port));
                google.runGoogleCredentialCheck();
                LOGGER.debug("Credential check over.");
                // copy credentials to the correct directory
                final File source = GOOGLE_LOCAL_FOLDER.getValue()
                        .map(File::new)
                        .filter(File::isDirectory)
                        .orElseThrow(() -> new IllegalStateException("Google credentials folder is not proper."));
                final File target = new File(INSTALL_PATH, source.getName());
                LOGGER.debug("Will copy {} to {}.", source, target);
                FileUtils.moveDirectory(source, target);
            }
            return cli;
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed configuring Google account.", ex);
        }
    }

    RoboZonkyInstallerListener.OS getOperatingSystem() {
        return operatingSystem;
    }

    private void prepareRunScript(final CommandLinePart commandLine) {
        final RunScriptGenerator generator = operatingSystem == RoboZonkyInstallerListener.OS.WINDOWS ?
                RunScriptGenerator.forWindows(DIST_PATH, CLI_CONFIG_FILE)
                : RunScriptGenerator.forUnix(DIST_PATH, CLI_CONFIG_FILE);
        final File runScript = generator.apply(commandLine);
        final File distRunScript = generator.getChildRunScript();
        Stream.of(runScript, distRunScript).forEach(script -> {
            final boolean success = script.setExecutable(true);
            LOGGER.info("Made '{}' executable: {}.", script, success);
        });
        if (operatingSystem == RoboZonkyInstallerListener.OS.LINUX) {
            prepareLinuxServices(runScript);
        }
    }

    @Override
    public void afterPacks(final List<Pack> packs, final ProgressListener progressListener) {
        try {
            progressListener.startAction("Konfigurace RoboZonky", 8);
            progressListener.nextStep("Příprava strategie.", 1, 1);
            final CommandLinePart strategyConfig = prepareStrategy();
            progressListener.nextStep("Příprava nastavení e-mailu.", 2, 1);
            final CommandLinePart emailConfig = prepareEmailConfiguration();
            progressListener.nextStep("Příprava Google účtu.", 3, 1);
            final CommandLinePart stonky = prepareStonky();
            progressListener.nextStep("Příprava nastavení JMX.", 4, 1);
            final CommandLinePart jmx = prepareJmx();
            progressListener.nextStep("Příprava nastavení Zonky.", 5, 1);
            final CommandLinePart core = prepareCore();
            progressListener.nextStep("Příprava nastavení logování.", 6, 1);
            final CommandLinePart logging = prepareLogging();
            progressListener.nextStep("Generování parametrů příkazové řádky.", 7, 1);
            final CommandLinePart result = prepareCommandLine(strategyConfig, emailConfig, stonky, jmx, core, logging);
            progressListener.nextStep("Generování spustitelného souboru.", 8, 1);
            prepareRunScript(result);
            progressListener.stopAction();
        } catch (final Exception ex) {
            LOGGER.error("Uncaught exception.", ex);
            throw new IllegalStateException("Uncaught exception.", ex);
        }
    }

    enum OS {
        WINDOWS,
        LINUX,
        OTHER
    }
}
