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
import java.net.URL;
import java.nio.file.Files;

import com.github.robozonky.internal.api.Defaults;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class StrategySettingsValidatorTest {

    private static InstallData mockInstallData() {
        final InstallData data = mock(InstallData.class);
        when(data.getVariable(eq(Variables.INSTALL_PATH.getKey())))
                .thenReturn(new File("target/").getAbsolutePath());
        return data;
    }

    private static InstallData mockInstallData(final File f) {
        final InstallData data = StrategySettingsValidatorTest.mockInstallData();
        when(data.getVariable(eq(Variables.STRATEGY_TYPE.getKey()))).thenReturn("file");
        when(data.getVariable(eq(Variables.STRATEGY_SOURCE.getKey())))
                .thenReturn(f.getAbsolutePath());
        return data;
    }

    private static InstallData mockInstallData(final URL u) {
        final InstallData data = StrategySettingsValidatorTest.mockInstallData();
        when(data.getVariable(eq(Variables.STRATEGY_TYPE.getKey()))).thenReturn("url");
        when(data.getVariable(eq(Variables.STRATEGY_SOURCE.getKey())))
                .thenReturn(u.toExternalForm());
        return data;
    }

    @AfterEach
    void resetDataTransfer() {
        RoboZonkyInstallerListener.resetInstallData();
    }

    @Test
    void messages() {
        final DataValidator validator = new StrategySettingsValidator();
        assertSoftly(softly -> {
            softly.assertThat(validator.getDefaultAnswer()).isFalse();
            softly.assertThat(validator.getWarningMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEqualTo(validator.getWarningMessageId());
        });
    }

    @Test
    void wrongData() {
        final DataValidator validator = new StrategySettingsValidator();
        assertThat(validator.validateData(StrategySettingsValidatorTest.mockInstallData()))
                .isEqualTo(DataValidator.Status.WARNING);
    }

    @Test
    void fileMissing() throws IOException {
        final File f = File.createTempFile("robozonky-", ".cfg");
        Assumptions.assumeTrue(f.delete());
        final InstallData d = StrategySettingsValidatorTest.mockInstallData(f);
        // execute sut
        final DataValidator validator = new StrategySettingsValidator();
        final DataValidator.Status result = validator.validateData(d);
        // execute test
        assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }

    @Test
    void fileOk() throws IOException {
        final File f = File.createTempFile("robozonky-", ".cfg");
        Files.write(f.toPath(), "Robot má udržovat konzervativní portfolio.".getBytes(Defaults.CHARSET));
        final InstallData d = StrategySettingsValidatorTest.mockInstallData(f);
        // execute sut
        final DataValidator validator = new StrategySettingsValidator();
        final DataValidator.Status result = validator.validateData(d);
        // execute test
        assertThat(result).isEqualTo(DataValidator.Status.OK);
    }

    @Test
    void urlOk() throws IOException {
        final File f = File.createTempFile("robozonky-", ".cfg");
        Files.write(f.toPath(), "Robot má udržovat konzervativní portfolio.".getBytes(Defaults.CHARSET));
        final InstallData d = StrategySettingsValidatorTest.mockInstallData(f.toURI().toURL());
        // execute sut
        final DataValidator validator = new StrategySettingsValidator();
        final DataValidator.Status result = validator.validateData(d);
        // execute test
        assertThat(result).isEqualTo(DataValidator.Status.OK);
    }

    @Test
    void urlNoContent() throws IOException {
        final File f = File.createTempFile("robozonky-", ".cfg");
        final InstallData d = StrategySettingsValidatorTest.mockInstallData(f.toURI().toURL());
        // execute sut
        final DataValidator validator = new StrategySettingsValidator();
        final DataValidator.Status result = validator.validateData(d);
        // execute test
        assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }

    @Test
    void urlWrong() throws IOException {
        final File f = File.createTempFile("robozonky-", ".cfg");
        Assumptions.assumeTrue(f.delete());
        final InstallData d = StrategySettingsValidatorTest.mockInstallData(f.toURI().toURL());
        // execute sut
        final DataValidator validator = new StrategySettingsValidator();
        final DataValidator.Status result = validator.validateData(d);
        // execute test
        assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }
}
