/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.installer.panels;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class ZonkySettingsValidatorTest {

    private static final String USERNAME = "someone@somewhere.cz", PASSWORD = UUID.randomUUID().toString();

    private static ZonkyApiToken mockToken(final ZonkyApiToken token) {
        return token == null ? ArgumentMatchers.any() : ArgumentMatchers.eq(token);
    }

    private static ApiProvider mockApiProvider(final OAuth oauth, final ZonkyApiToken token, final Zonky zonky) {
        final ApiProvider api = Mockito.mock(ApiProvider.class);
        Mockito.when(api.oauth(ArgumentMatchers.any(Function.class))).then(i -> {
            final Function f = i.getArgument(0);
            return f.apply(oauth);
        });
        Mockito.doAnswer(i -> {
            final Function f = i.getArgument(1);
            return f.apply(zonky);
        }).when(api).authenticated(mockToken(token), ArgumentMatchers.any(Function.class));
        Mockito.doAnswer(i -> {
            final Consumer f = i.getArgument(1);
            f.accept(zonky);
            return null;
        }).when(api).authenticated(mockToken(token), ArgumentMatchers.any(Consumer.class));
        return api;
    }

    private static ApiProvider mockApiProvider() {
        return mockApiProvider(Mockito.mock(OAuth.class));
    }

    private static ApiProvider mockApiProvider(final OAuth oAuth) {
        return mockApiProvider(oAuth, null, Mockito.mock(Zonky.class));
    }

    private static InstallData mockInstallData() {
        final InstallData d = Mockito.mock(InstallData.class);
        Mockito.when(d.getVariable(Variables.ZONKY_USERNAME.getKey())).thenReturn(ZonkySettingsValidatorTest.USERNAME);
        Mockito.when(d.getVariable(Variables.ZONKY_PASSWORD.getKey())).thenReturn(ZonkySettingsValidatorTest.PASSWORD);
        return d;
    }

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

    @Test
    public void properLogin() {
        // mock data
        final ZonkyApiToken token = Mockito.mock(ZonkyApiToken.class);
        final OAuth oauth = Mockito.mock(OAuth.class);
        Mockito.when(oauth.login(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(token);
        final Zonky zonky = Mockito.mock(Zonky.class);
        final ApiProvider provider = mockApiProvider(oauth, token, zonky);
        // execute SUT
        final ZonkySettingsValidator validator = new ZonkySettingsValidator(() -> provider);
        final InstallData d = ZonkySettingsValidatorTest.mockInstallData();
        final DataValidator.Status result = validator.validateData(d);
        // test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.OK);
        Mockito.verify(oauth)
                .login(ArgumentMatchers.eq(ZonkySettingsValidatorTest.USERNAME),
                       ArgumentMatchers.eq(ZonkySettingsValidatorTest.PASSWORD.toCharArray()));
        Mockito.verify(zonky).logout();
    }

    @Test
    public void warning() {
        // mock data
        final OAuth oauth = Mockito.mock(OAuth.class);
        Mockito.when(oauth.login(ArgumentMatchers.any(), ArgumentMatchers.any())).thenThrow(
                new IllegalStateException());
        final ApiProvider provider = mockApiProvider(oauth);
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
        final OAuth oauth = Mockito.mock(OAuth.class);
        Mockito.when(oauth.login(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenThrow(new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR));
        final ApiProvider provider = mockApiProvider(oauth);
        final InstallData d = ZonkySettingsValidatorTest.mockInstallData();
        // execute SUT
        final ZonkySettingsValidator validator = new ZonkySettingsValidator(() -> provider);
        final DataValidator.Status result = validator.validateData(d);
        // test
        Assertions.assertThat(result).isEqualTo(DataValidator.Status.ERROR);
    }
}
