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
import java.net.MalformedURLException;
import java.net.URL;

import com.github.robozonky.cli.Feature;
import com.github.robozonky.cli.StrategyValidationFeature;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;

public class StrategySettingsValidator extends AbstractValidator {

    private static Feature getFeature(final String type, final String location) throws MalformedURLException {
        switch (type) {
            case "file":
                return new StrategyValidationFeature(new File(location));
            case "url":
                return new StrategyValidationFeature(new URL(location));
            default:
                throw new IllegalStateException("Impossible.");
        }
    }

    @Override
    public DataValidator.Status validateDataPossiblyThrowingException(final InstallData installData) {
        RoboZonkyInstallerListener.setInstallData(installData);
        final String type = Variables.STRATEGY_TYPE.getValue(installData);
        final String strategySource = Variables.STRATEGY_SOURCE.getValue(installData);
        try {
            final Feature f = getFeature(type, strategySource);
            f.setup();
            f.test();
            return DataValidator.Status.OK;
        } catch (final Exception ex) {
            logger.warn("Strategy invalid: {}.", strategySource, ex);
            return DataValidator.Status.WARNING;
        }
    }

    @Override
    public String getErrorMessageId() {
        return "Zadaná strategie neexistuje nebo není platná.";
    }

    @Override
    public String getWarningMessageId() {
        return "Zadaná strategie neexistuje nebo není platná. RoboZonky nemusí fungovat správně.";
    }
}
