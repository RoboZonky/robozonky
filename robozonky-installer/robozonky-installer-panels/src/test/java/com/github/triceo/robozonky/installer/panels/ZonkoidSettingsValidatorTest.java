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

import java.util.Collections;
import java.util.Optional;

import com.github.triceo.robozonky.api.confirmations.Confirmation;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.confirmations.ConfirmationType;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class ZonkoidSettingsValidatorTest {

    private static final String USER = "someone@somewhere.cz", TOKEN = String.valueOf((int)(Math.random() * 100_000));

    @Test
    public void messages() {
        final DataValidator validator = new ZonkoidSettingsValidator();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(validator.getDefaultAnswer()).isFalse();
            softly.assertThat(validator.getWarningMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEqualTo(validator.getWarningMessageId());
        });
    }

    @Test
    public void marketplaceFail() {
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        Mockito.doThrow(new IllegalStateException("Testing")).when(provider).anonymous();
        final DataValidator validator = new ZonkoidSettingsValidator(() -> provider);
        final DataValidator.Status result = validator.validateData(Mockito.mock(InstallData.class));
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }

    @Test
    public void marketplaceWithoutLoans() {
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getLoans()).thenReturn(Collections.emptyList());
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        Mockito.when(provider.anonymous()).thenReturn(new ApiProvider.ApiWrapper<>(api));
        // execute SUT
        final DataValidator validator = new ZonkoidSettingsValidator(() -> provider);
        final DataValidator.Status result = validator.validateData(Mockito.mock(InstallData.class));
        // run test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }

    private static ApiProvider mockApiThatReturnsOneLoan() {
        final Loan l = Mockito.mock(Loan.class);
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.doReturn(Collections.singletonList(l)).when(api).getLoans();
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        Mockito.doReturn(new ApiProvider.ApiWrapper<>(api)).when(provider).anonymous();
        return provider;
    }

    @Test
    public void zonkoidMissing() {
        // execute SUT
        final DataValidator validator =
                new ZonkoidSettingsValidator(ZonkoidSettingsValidatorTest::mockApiThatReturnsOneLoan, Optional::empty);
        final DataValidator.Status result = validator.validateData(Mockito.mock(InstallData.class));
        // run test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.ERROR);
    }

    private static InstallData mockInstallData() {
        final InstallData d = Mockito.mock(InstallData.class);
        Mockito.when(d.getVariable(Variables.ZONKY_USERNAME.getKey())).thenReturn(ZonkoidSettingsValidatorTest.USER);
        Mockito.when(d.getVariable(Variables.ZONKOID_TOKEN.getKey())).thenReturn(ZonkoidSettingsValidatorTest.TOKEN);
        return d;
    }

    @Test
    public void zonkoidPresentButNotConfirming() {
        final ConfirmationProvider cp = Mockito.mock(ConfirmationProvider.class);
        Mockito.when(cp.requestConfirmation(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt())).thenReturn(Optional.empty());
        final InstallData d = ZonkoidSettingsValidatorTest.mockInstallData();
        // execute SUT
        final DataValidator validator =
                new ZonkoidSettingsValidator(ZonkoidSettingsValidatorTest::mockApiThatReturnsOneLoan,
                        () -> Optional.of(cp));
        final DataValidator.Status result = validator.validateData(d);
        // run test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.ERROR);
    }

    @Test
    public void zonkoidPresentButRejecting() {
        final ConfirmationProvider cp = Mockito.mock(ConfirmationProvider.class);
        Mockito.when(cp.requestConfirmation(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt())).thenReturn(Optional.of(new Confirmation(ConfirmationType.REJECTED)));
        final InstallData d = ZonkoidSettingsValidatorTest.mockInstallData();
        // execute SUT
        final DataValidator validator =
                new ZonkoidSettingsValidator(ZonkoidSettingsValidatorTest::mockApiThatReturnsOneLoan,
                        () -> Optional.of(cp));
        final DataValidator.Status result = validator.validateData(d);
        // run test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }

    @Test
    public void zonkoidProper() {
        final ConfirmationProvider cp = Mockito.mock(ConfirmationProvider.class);
        Mockito.when(cp.requestConfirmation(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt())).thenReturn(Optional.of(new Confirmation(ConfirmationType.DELEGATED)));
        final InstallData d = ZonkoidSettingsValidatorTest.mockInstallData();
        // execute SUT
        final DataValidator validator =
                new ZonkoidSettingsValidator(ZonkoidSettingsValidatorTest::mockApiThatReturnsOneLoan,
                        () -> Optional.of(cp));
        final DataValidator.Status result = validator.validateData(d);
        // run test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.OK);
    }

}
