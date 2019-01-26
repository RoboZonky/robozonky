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

import java.util.Optional;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class ZonkoidSettingsValidatorTest {

    private static final String USER = "someone@somewhere.cz", TOKEN = String.valueOf((int) (Math.random() * 100_000));

    @Test
    void messages() {
        final DataValidator validator = new ZonkoidSettingsValidator();
        assertSoftly(softly -> {
            softly.assertThat(validator.getDefaultAnswer()).isFalse();
            softly.assertThat(validator.getWarningMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEqualTo(validator.getWarningMessageId());
        });
    }

    @Test
    void zonkoidMissing() {
        // execute SUT
        final DataValidator validator =
                new ZonkoidSettingsValidator(Optional::empty);
        final DataValidator.Status result = validator.validateData(mock(InstallData.class));
        // run test
        assertThat(result).isEqualTo(DataValidator.Status.ERROR);
    }

    private static InstallData mockInstallData() {
        final InstallData d = mock(InstallData.class);
        when(d.getVariable(Variables.ZONKY_USERNAME.getKey())).thenReturn(ZonkoidSettingsValidatorTest.USER);
        when(d.getVariable(Variables.ZONKOID_TOKEN.getKey())).thenReturn(ZonkoidSettingsValidatorTest.TOKEN);
        return d;
    }

    @Test
    void zonkoidPresentButRejecting() {
        final ConfirmationProvider cp = mock(ConfirmationProvider.class);
        when(cp.requestConfirmation(any(), anyInt(),
                                    anyInt())).thenReturn(false);
        final InstallData d = ZonkoidSettingsValidatorTest.mockInstallData();
        // execute SUT
        final DataValidator validator = new ZonkoidSettingsValidator(() -> Optional.of(cp));
        final DataValidator.Status result = validator.validateData(d);
        // run test
        assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }

    @Test
    void zonkoidProper() {
        final ConfirmationProvider cp = mock(ConfirmationProvider.class);
        when(cp.requestConfirmation(any(), anyInt(),
                                    anyInt())).thenReturn(true);
        final InstallData d = ZonkoidSettingsValidatorTest.mockInstallData();
        // execute SUT
        final DataValidator validator = new ZonkoidSettingsValidator(() -> Optional.of(cp));
        final DataValidator.Status result = validator.validateData(d);
        // run test
        assertThat(result).isEqualTo(DataValidator.Status.OK);
    }
}
