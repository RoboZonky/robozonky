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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;

class InstallDirValidatorTest {

    private static InstallData mockBaseData(final String installPath) {
        final InstallData data = mock(InstallData.class);
        when(data.getVariable(Variables.INSTALL_PATH.getKey()))
            .thenReturn(new File(installPath).getAbsolutePath());
        return data;
    }

    @Test
    void correctDirLinux() {
        final InstallData d = InstallDirValidatorTest.mockBaseData("/home/lpetrovi/RoboZonky/4.0.0-SNAPSHOT");
        final InstallDirValidator v = new InstallDirValidator();
        assertThat(v.validateData(d)).isEqualTo(DataValidator.Status.OK);
    }

    @Test
    void correctDirWindows() {
        final InstallData d = InstallDirValidatorTest.mockBaseData("C:\\RoboZonky\\4.0.0-SNAPSHOT");
        final InstallDirValidator v = new InstallDirValidator();
        assertThat(v.validateData(d)).isEqualTo(DataValidator.Status.OK);
    }

    @Test
    void wrongDir() {
        final InstallData d = InstallDirValidatorTest.mockBaseData("C:\\Program Files\\RoboZonky\\4.0.0-SNAPSHOT");
        final InstallDirValidator v = new InstallDirValidator();
        assertThat(v.validateData(d)).isEqualTo(DataValidator.Status.ERROR);
        assertThat(v.getErrorMessageId()).isNotEmpty();
    }
}
