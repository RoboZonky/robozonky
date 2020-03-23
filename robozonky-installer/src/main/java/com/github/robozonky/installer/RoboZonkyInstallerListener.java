/*
 * Copyright 2020 The RoboZonky Project
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.installer.scripts.RunScriptGenerator;
import com.github.robozonky.installer.scripts.ServiceGenerator;
import com.github.robozonky.internal.Settings;
import com.github.robozonky.internal.util.FileUtil;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Pack;
import com.izforge.izpack.api.event.AbstractInstallerListener;
import com.izforge.izpack.api.event.ProgressListener;

public final class RoboZonkyInstallerListener extends AbstractInstallerListener {

    private static final Logger LOGGER = LogManager.getLogger(RoboZonkyInstallerListener.class);
    private static File DIST_PATH;
    private static File SETTINGS_FILE;
    private static File LOG4J2_CONFIG_FILE;
    private static File KEYSTORE_SOURCE;
    private static File KEYSTORE_TARGET;
    private static char[] KEYSTORE_SECRET;
    private static InstallData DATA;
    static File INSTALL_PATH;
    static File JMX_PROPERTIES_FILE;
    static File EMAIL_CONFIG_FILE;
    static File CLI_CONFIG_FILE;

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
     * 
     * @param os Fake operating system used for testing.
     */
    RoboZonkyInstallerListener(final RoboZonkyInstallerListener.OS os) {
        operatingSystem = os;
    }

    static void setKeystoreInformation(final File keystore, final char... keystorePassword) {
        KEYSTORE_SOURCE = keystore;
        KEYSTORE_SECRET = keystorePassword;
    }

    /**
     * This is a dirty ugly hack to workaround a bug in IZPack's Picocontainer. If we had the proper constructor to
     * accept {@link InstallData}, Picocontainer would have thrown some weird exception.
     * <p>
     * Therefore we share the data this way - through the last panel before the actual installation starts.
     * <p>
     * See more at https://izpack.atlassian.net/browse/IZPACK-1403.
     * 
     * @param data Installer data to store.
     */
    static void setInstallData(final InstallData data) {
        RoboZonkyInstallerListener.DATA = data;
        INSTALL_PATH = new File(Variables.INSTALL_PATH.getValue(DATA));
        DIST_PATH = new File(INSTALL_PATH, "Dist/");
        KEYSTORE_TARGET = new File(INSTALL_PATH, "robozonky.keystore");
        JMX_PROPERTIES_FILE = new File(INSTALL_PATH, "management.properties");
        EMAIL_CONFIG_FILE = new File(INSTALL_PATH, "robozonky-notifications.cfg");
        SETTINGS_FILE = new File(INSTALL_PATH, "robozonky.properties");
        CLI_CONFIG_FILE = new File(INSTALL_PATH, "robozonky.cli");
        LOG4J2_CONFIG_FILE = new File(INSTALL_PATH, "log4j2.xml");
    }

    static void resetInstallData() {
        RoboZonkyInstallerListener.DATA = null;
        INSTALL_PATH = null;
        DIST_PATH = null;
        KEYSTORE_TARGET = null;
        JMX_PROPERTIES_FILE = null;
        EMAIL_CONFIG_FILE = null;
        SETTINGS_FILE = null;
        CLI_CONFIG_FILE = null;
        LOG4J2_CONFIG_FILE = null;
    }

    private static void primeKeyStore() throws IOException {
        Files.deleteIfExists(KEYSTORE_TARGET.toPath()); // re-install into the same directory otherwise fails
        Files.copy(KEYSTORE_SOURCE.toPath(), KEYSTORE_TARGET.toPath());
    }

    static CommandLinePart prepareCore() throws IOException {
        final CommandLinePart cli = new CommandLinePart()
            .setOption("-g", KEYSTORE_TARGET.getAbsolutePath())
            .setOption("-p", String.valueOf(KEYSTORE_SECRET));
        if (Boolean.valueOf(Variables.IS_DRY_RUN.getValue(DATA))) {
            cli.setOption("-d");
            cli.setJvmArgument("Xmx128m"); // more memory for the JFR recording
            cli.setJvmArgument("XX:StartFlightRecording=disk=true,dumponexit=true,maxage=1d,path-to-gc-roots=true");
        } else {
            cli.setJvmArgument("Xmx64m");
        }
        primeKeyStore();
        return cli;
    }

    private static File assembleCliFile(final CommandLinePart... source) throws IOException {
        // assemble the CLI
        final CommandLinePart cli = new CommandLinePart();
        Stream.of(source)
            .forEach(c -> Util.copyOptions(c, cli));
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
                FileUtil.configurePermissions(strategyFile, false);
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
                return EMAIL_CONFIG_FILE.toURI()
                    .toURL();
            case "url":
                return new URL(Variables.EMAIL_CONFIGURATION_SOURCE.getValue(DATA));
            default:
                final Properties props = Util.configureEmailNotifications(DATA);
                Util.writeOutProperties(props, EMAIL_CONFIG_FILE);
                return EMAIL_CONFIG_FILE.toURI()
                    .toURL();
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

    private static CommandLinePart prepareCommandLine(final CommandLinePart strategy, final CommandLinePart emailConfig,
            final CommandLinePart jmxConfig, final CommandLinePart core,
            final CommandLinePart logging) {
        try {
            final File cliConfigFile = assembleCliFile(core, strategy, emailConfig);
            // have the CLI file loaded during RoboZonky startup
            final CommandLinePart commandLine = new CommandLinePart()
                .setOption("@" + cliConfigFile.getAbsolutePath())
                .setProperty(Settings.FILE_LOCATION_PROPERTY, SETTINGS_FILE.getAbsolutePath())
                .setEnvironmentVariable("JAVA_HOME", "");
            // now proceed to set all system properties and settings
            final Properties settings = new Properties();
            Util.processCommandLine(commandLine, settings, strategy, jmxConfig, core, logging);
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
            final InputStream log4j2config = RoboZonkyInstallerListener.class.getResourceAsStream("/log4j2.xml");
            FileUtils.copyInputStreamToFile(log4j2config, LOG4J2_CONFIG_FILE);
            FileUtil.configurePermissions(LOG4J2_CONFIG_FILE, false);
            return new CommandLinePart()
                .setProperty("log4j.configurationFile", LOG4J2_CONFIG_FILE.getAbsolutePath());
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed copying Log4j configuration file.", ex);
        }
    }

    RoboZonkyInstallerListener.OS getOperatingSystem() {
        return operatingSystem;
    }

    private void prepareRunScript(final CommandLinePart commandLine) {
        final RunScriptGenerator generator = operatingSystem == RoboZonkyInstallerListener.OS.WINDOWS
                ? RunScriptGenerator.forWindows(DIST_PATH, CLI_CONFIG_FILE)
                : RunScriptGenerator.forUnix(DIST_PATH, CLI_CONFIG_FILE);
        final File runScript = generator.apply(commandLine);
        final File distRunScript = generator.getChildRunScript();
        Stream.of(runScript, distRunScript)
            .forEach(script -> {
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
            progressListener.startAction("Konfigurace RoboZonky", 7);
            progressListener.nextStep("Příprava strategie.", 1, 1);
            final CommandLinePart strategyConfig = prepareStrategy();
            progressListener.nextStep("Příprava nastavení e-mailu.", 2, 1);
            final CommandLinePart emailConfig = prepareEmailConfiguration();
            progressListener.nextStep("Příprava nastavení JMX.", 3, 1);
            final CommandLinePart jmx = prepareJmx();
            progressListener.nextStep("Příprava nastavení Zonky.", 4, 1);
            final CommandLinePart core = prepareCore();
            progressListener.nextStep("Příprava nastavení logování.", 5, 1);
            final CommandLinePart logging = prepareLogging();
            progressListener.nextStep("Generování parametrů příkazové řádky.", 6, 1);
            final CommandLinePart result = prepareCommandLine(strategyConfig, emailConfig, jmx, core, logging);
            progressListener.nextStep("Generování spustitelného souboru.", 7, 1);
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
