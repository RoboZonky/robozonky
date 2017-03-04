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
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;

public class StrategySettingsValidator implements DataValidator {

    private static final Logger LOGGER = Logger.getLogger(StrategySettingsValidator.class.getCanonicalName());

    @Override
    public DataValidator.Status validateData(final InstallData installData) {
        RoboZonkyInstallerListener.setInstallData(installData);
        final String type = Variables.STRATEGY_TYPE.getValue(installData);
        final String strategySource = Variables.STRATEGY_SOURCE.getValue(installData);
        if (Objects.equals(type, "file")) {
            final File f = new File(strategySource);
            if (f.canRead()) {
                return DataValidator.Status.OK;
            } else {
                return DataValidator.Status.WARNING;
            }
        } else if (Objects.equals(type, "url")) {
            try (final InputStream is = new URL(strategySource).openStream()) {
                if (is.available() > 0) {
                    return DataValidator.Status.OK;
                } else {
                    return DataValidator.Status.WARNING;
                }
            } catch (final Exception ex) {
                StrategySettingsValidator.LOGGER.log(Level.WARNING, "Cannot read URL.", ex);
                return DataValidator.Status.WARNING;
            }
        } else {
            return DataValidator.Status.ERROR;
        }
    }

    @Override
    public String getErrorMessageId() {
        return "Nebyla zadána strategie.";
    }

    @Override
    public String getWarningMessageId() {
        return "Zadaná strategie neexistuje. RoboZonky nemusí fungovat správně.";
    }

    @Override
    public boolean getDefaultAnswer() {
        return false;
    }
}
