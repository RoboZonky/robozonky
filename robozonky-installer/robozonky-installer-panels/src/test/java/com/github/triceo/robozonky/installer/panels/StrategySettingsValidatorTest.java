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
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;

import com.github.triceo.robozonky.internal.api.Defaults;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class StrategySettingsValidatorTest {

    @After
    public void resetDataTransfer() {
        RoboZonkyInstallerListener.resetInstallData();
    }

    @Test
    public void messages() {
        final DataValidator validator = new StrategySettingsValidator();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(validator.getDefaultAnswer()).isFalse();
            softly.assertThat(validator.getWarningMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEqualTo(validator.getWarningMessageId());
        });
    }

    private static InstallData mockInstallData() {
        final InstallData data = Mockito.mock(InstallData.class);
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.INSTALL_PATH.getKey())))
                .thenReturn(new File("target/").getAbsolutePath());
        return data;
    }

    private static InstallData mockInstallData(final File f) {
        final InstallData data = StrategySettingsValidatorTest.mockInstallData();
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.STRATEGY_TYPE.getKey()))).thenReturn("file");
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.STRATEGY_SOURCE.getKey())))
                .thenReturn(f.getAbsolutePath());
        return data;
    }

    private static InstallData mockInstallData(final URL u) {
        final InstallData data = StrategySettingsValidatorTest.mockInstallData();
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.STRATEGY_TYPE.getKey()))).thenReturn("url");
        Mockito.when(data.getVariable(ArgumentMatchers.eq(Variables.STRATEGY_SOURCE.getKey())))
                .thenReturn(u.toExternalForm());
        return data;
    }

    @Test
    public void wrongData() {
        final DataValidator validator = new StrategySettingsValidator();
        Assertions.assertThat(validator.validateData(StrategySettingsValidatorTest.mockInstallData()))
                .isEqualTo(DataValidator.Status.ERROR);
    }

    @Test
    public void fileOk() throws IOException {
        final File f = File.createTempFile("robozonky-", ".cfg");
        final InstallData d = StrategySettingsValidatorTest.mockInstallData(f);
        // execute sut
        final DataValidator validator = new StrategySettingsValidator();
        final DataValidator.Status result = validator.validateData(d);
        // execute test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.OK);
        Assertions.assertThat(RoboZonkyInstallerListener.INSTALL_PATH).isNotNull();
    }

    @Test
    public void fileMissing() throws IOException {
        final File f = File.createTempFile("robozonky-", ".cfg");
        Assume.assumeTrue(f.delete());
        final InstallData d = StrategySettingsValidatorTest.mockInstallData(f);
        // execute sut
        final DataValidator validator = new StrategySettingsValidator();
        final DataValidator.Status result = validator.validateData(d);
        // execute test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }

    @Test
    public void urlOk() throws IOException {
        final File f = File.createTempFile("robozonky-", ".cfg");
        Files.write(f.toPath(), Collections.singleton("Content"), Defaults.CHARSET);
        final InstallData d = StrategySettingsValidatorTest.mockInstallData(f.toURI().toURL());
        // execute sut
        final DataValidator validator = new StrategySettingsValidator();
        final DataValidator.Status result = validator.validateData(d);
        // execute test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.OK);
    }

    @Test
    public void urlNoContent() throws IOException {
        final File f = File.createTempFile("robozonky-", ".cfg");
        final InstallData d = StrategySettingsValidatorTest.mockInstallData(f.toURI().toURL());
        // execute sut
        final DataValidator validator = new StrategySettingsValidator();
        final DataValidator.Status result = validator.validateData(d);
        // execute test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }

    @Test
    public void urlWrong() throws IOException {
        final File f = File.createTempFile("robozonky-", ".cfg");
        Assume.assumeTrue(f.delete());
        final InstallData d = StrategySettingsValidatorTest.mockInstallData(f.toURI().toURL());
        // execute sut
        final DataValidator validator = new StrategySettingsValidator();
        final DataValidator.Status result = validator.validateData(d);
        // execute test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }

}
