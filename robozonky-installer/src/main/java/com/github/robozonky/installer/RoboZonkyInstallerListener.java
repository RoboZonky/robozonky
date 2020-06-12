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

import static com.github.robozonky.installer.Variables.EMAIL_CONFIGURATION_SOURCE;
import static com.github.robozonky.installer.Variables.EMAIL_CONFIGURATION_TYPE;
import static com.github.robozonky.installer.Variables.IS_DRY_RUN;
import static com.github.robozonky.installer.Variables.IS_EMAIL_ENABLED;
import static com.github.robozonky.installer.Variables.IS_JMX_ENABLED;
import static com.github.robozonky.installer.Variables.IS_JMX_SECURITY_ENABLED;
import static com.github.robozonky.installer.Variables.JMX_HOSTNAME;
import static com.github.robozonky.installer.Variables.JMX_PORT;
import static com.github.robozonky.installer.Variables.STRATEGY_SOURCE;
import static com.github.robozonky.installer.Variables.STRATEGY_TYPE;
import static java.lang.Boolean.parseBoolean;

import java.io.File;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.installer.configuration.ConfigurationModel;
import com.github.robozonky.installer.configuration.NotificationConfiguration;
import com.github.robozonky.installer.configuration.PropertyConfiguration;
import com.github.robozonky.installer.configuration.StrategyConfiguration;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Pack;
import com.izforge.izpack.api.event.AbstractInstallerListener;
import com.izforge.izpack.api.event.ProgressListener;

final class RoboZonkyInstallerListener extends AbstractInstallerListener {

    private static final Logger LOGGER = LogManager.getLogger(RoboZonkyInstallerListener.class);
    static File INSTALL_PATH;
    private static File DIST_PATH;
    private static File KEYSTORE_SOURCE;
    private static char[] KEYSTORE_SECRET;
    private static InstallData DATA;

    private final boolean isUnix;

    public RoboZonkyInstallerListener() {
        this(!SystemUtils.IS_OS_WINDOWS);
    }

    public RoboZonkyInstallerListener(boolean isUnix) {
        this.isUnix = isUnix;
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
        DIST_PATH = new File(INSTALL_PATH, "dist/");
    }

    static void resetInstallData() {
        RoboZonkyInstallerListener.DATA = null;
        INSTALL_PATH = null;
        DIST_PATH = null;
    }

    static PropertyConfiguration prepareCore() {
        if (parseBoolean(IS_DRY_RUN.getValue(DATA))) {
            return PropertyConfiguration.applicationDryRun(KEYSTORE_SOURCE.toPath(), KEYSTORE_SECRET);
        } else {
            return PropertyConfiguration.applicationReal(KEYSTORE_SOURCE.toPath(), KEYSTORE_SECRET);
        }
    }

    static StrategyConfiguration prepareStrategy() {
        final String content = STRATEGY_SOURCE.getValue(DATA);
        if (Objects.equals(STRATEGY_TYPE.getValue(DATA), "file")) {
            return StrategyConfiguration.local(content);
        } else {
            return StrategyConfiguration.remote(content);
        }
    }

    static NotificationConfiguration prepareEmailConfiguration() {
        if (!parseBoolean(IS_EMAIL_ENABLED.getValue(DATA))) {
            return NotificationConfiguration.disabled();
        }
        switch (EMAIL_CONFIGURATION_TYPE.getValue(DATA)) {
            case "file":
                return NotificationConfiguration.reuse(EMAIL_CONFIGURATION_SOURCE.getValue(DATA));
            case "url":
                return NotificationConfiguration.remote(EMAIL_CONFIGURATION_SOURCE.getValue(DATA));
            default:
                return NotificationConfiguration.create(Util.configureEmailNotifications(DATA));
        }
    }

    static PropertyConfiguration prepareJmx() {
        final boolean isJmxEnabled = parseBoolean(IS_JMX_ENABLED.getValue(DATA));
        if (!isJmxEnabled) { // ignore JMX
            return PropertyConfiguration.disabledJmx();
        }
        final int port = Integer.parseInt(JMX_PORT.getValue(DATA));
        return PropertyConfiguration.enabledJmx(JMX_HOSTNAME.getValue(DATA), port,
                parseBoolean(IS_JMX_SECURITY_ENABLED.getValue(DATA)));
    }

    @Override
    public void afterPacks(final List<Pack> packs, final ProgressListener progressListener) {
        try {
            progressListener.startAction("Konfigurace RoboZonky", 5);
            progressListener.nextStep("Příprava strategie.", 1, 1);
            final StrategyConfiguration strategyConfig = prepareStrategy();
            progressListener.nextStep("Příprava nastavení e-mailu.", 2, 1);
            final NotificationConfiguration emailConfig = prepareEmailConfiguration();
            progressListener.nextStep("Příprava nastavení JMX.", 3, 1);
            final PropertyConfiguration jmx = prepareJmx();
            progressListener.nextStep("Příprava nastavení RoboZonky.", 4, 1);
            final PropertyConfiguration core = prepareCore();
            progressListener.nextStep("Generování konfigurace RoboZonky.", 5, 1);
            ConfigurationModel configurationModel = ConfigurationModel.load(core, strategyConfig, emailConfig, jmx);
            configurationModel.materialize(DIST_PATH.toPath(), INSTALL_PATH.toPath(), isUnix);
            progressListener.stopAction();
        } catch (final Exception ex) {
            LOGGER.error("Uncaught exception.", ex);
            throw new IllegalStateException("Uncaught exception.", ex);
        }
    }

}
