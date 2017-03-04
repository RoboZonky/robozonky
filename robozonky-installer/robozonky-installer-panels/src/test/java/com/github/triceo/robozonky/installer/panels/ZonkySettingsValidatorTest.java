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

import java.util.UUID;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class ZonkySettingsValidatorTest {

    private static final String USERNAME = "someone@somewhere.cz", PASSWORD = UUID.randomUUID().toString();

    @Test
    public void messages() {
        final ZonkySettingsValidator validator = new ZonkySettingsValidator();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(validator.getDefaultAnswer()).isFalse();
            softly.assertThat(validator.getWarningMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEqualTo(validator.getWarningMessageId());
        });
    }

    private static InstallData mockInstallData() {
        final InstallData d = Mockito.mock(InstallData.class);
        Mockito.when(d.getVariable(Variables.ZONKY_USERNAME.getKey())).thenReturn(ZonkySettingsValidatorTest.USERNAME);
        Mockito.when(d.getVariable(Variables.ZONKY_PASSWORD.getKey())).thenReturn(ZonkySettingsValidatorTest.PASSWORD);
        return d;
    }

    @Test
    public void properLogin() {
        // mock data
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        final ZonkyApiToken token = Mockito.mock(ZonkyApiToken.class);
        final ZonkyOAuthApi oauth = Mockito.mock(ZonkyOAuthApi.class);
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(oauth.login(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenReturn(token);
        Mockito.when(provider.authenticated(ArgumentMatchers.eq(token)))
                .thenReturn(new ApiProvider.ApiWrapper<>(api));
        Mockito.when(provider.oauth()).thenReturn(new ApiProvider.ApiWrapper<>(oauth));
        // execute SUT
        final ZonkySettingsValidator validator = new ZonkySettingsValidator(() -> provider);
        final InstallData d = ZonkySettingsValidatorTest.mockInstallData();
        final DataValidator.Status result = validator.validateData(d);
        // test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.OK);
        Mockito.verify(oauth)
                .login(ArgumentMatchers.eq(ZonkySettingsValidatorTest.USERNAME),
                        ArgumentMatchers.eq(ZonkySettingsValidatorTest.PASSWORD), ArgumentMatchers.any(),
                        ArgumentMatchers.any());
        Mockito.verify(api).logout();
    }

    @Test
    public void warning() {
        // mock data
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        final ZonkyOAuthApi oauth = Mockito.mock(ZonkyOAuthApi.class);
        Mockito.when(oauth.login(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenThrow(new IllegalStateException());
        Mockito.when(provider.oauth()).thenReturn(new ApiProvider.ApiWrapper<>(oauth));
        final InstallData d = ZonkySettingsValidatorTest.mockInstallData();
        // execute SUT
        final ZonkySettingsValidator validator = new ZonkySettingsValidator(() -> provider);
        final DataValidator.Status result = validator.validateData(d);
        // test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }

    @Test
    public void error() {
        // mock data
        final ApiProvider provider = Mockito.mock(ApiProvider.class);
        final ZonkyOAuthApi oauth = Mockito.mock(ZonkyOAuthApi.class);
        Mockito.when(oauth.login(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenThrow(new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR));
        Mockito.when(provider.oauth()).thenReturn(new ApiProvider.ApiWrapper<>(oauth));
        final InstallData d = ZonkySettingsValidatorTest.mockInstallData();
        // execute SUT
        final ZonkySettingsValidator validator = new ZonkySettingsValidator(() -> provider);
        final DataValidator.Status result = validator.validateData(d);
        // test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.ERROR);
    }

}
