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

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class ZonkySettingsValidatorTest {

    private static final String USERNAME = "someone@somewhere.cz", PASSWORD = UUID.randomUUID().toString();

    @SuppressWarnings("unchecked")
    private static ApiProvider mockApiProvider(final OAuth oauth, final ZonkyApiToken token, final Zonky zonky) {
        final ApiProvider api = mock(ApiProvider.class);
        when(api.oauth(any(Function.class))).then(i -> {
            final Function<OAuth, ?> f = i.getArgument(0);
            return f.apply(oauth);
        });
        doAnswer(i -> {
            final Supplier<ZonkyApiToken> t = i.getArgument(1);
            assertThat(t.get()).isEqualTo(token);
            final Function<Zonky, ?> f = i.getArgument(0);
            return f.apply(zonky);
        }).when(api).call(any(Function.class), any());
        doAnswer(i -> {
            final Supplier<ZonkyApiToken> t = i.getArgument(1);
            assertThat(t.get()).isEqualTo(token);
            final Consumer<Zonky> f = i.getArgument(0);
            f.accept(zonky);
            return null;
        }).when(api).run(any(Consumer.class), any());
        return api;
    }

    private static ApiProvider mockApiProvider(final OAuth oAuth) {
        return mockApiProvider(oAuth, null, mock(Zonky.class));
    }

    private static InstallData mockInstallData() {
        final InstallData d = mock(InstallData.class);
        when(d.getVariable(Variables.ZONKY_USERNAME.getKey())).thenReturn(ZonkySettingsValidatorTest.USERNAME);
        when(d.getVariable(Variables.ZONKY_PASSWORD.getKey())).thenReturn(ZonkySettingsValidatorTest.PASSWORD);
        return d;
    }

    @Test
    void messages() {
        final ZonkySettingsValidator validator = new ZonkySettingsValidator();
        assertSoftly(softly -> {
            softly.assertThat(validator.getDefaultAnswer()).isFalse();
            softly.assertThat(validator.getWarningMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEmpty();
            softly.assertThat(validator.getErrorMessageId()).isNotEqualTo(validator.getWarningMessageId());
        });
    }

    @Test
    void properLogin() {
        // mock data
        final ZonkyApiToken token = mock(ZonkyApiToken.class);
        final OAuth oauth = mock(OAuth.class);
        when(oauth.login(any(), any())).thenReturn(token);
        final Zonky zonky = mock(Zonky.class);
        final ApiProvider provider = mockApiProvider(oauth, token, zonky);
        // execute SUT
        final ZonkySettingsValidator validator = new ZonkySettingsValidator(() -> provider);
        final InstallData d = ZonkySettingsValidatorTest.mockInstallData();
        final DataValidator.Status result = validator.validateData(d);
        // test
        assertThat(result).isEqualTo(DataValidator.Status.OK);
        verify(oauth)
                .login(eq(ZonkySettingsValidatorTest.USERNAME),
                       eq(ZonkySettingsValidatorTest.PASSWORD.toCharArray()));
        verify(zonky).logout();
    }

    @Test
    void warning() {
        // mock data
        final OAuth oauth = mock(OAuth.class);
        when(oauth.login(any(), any())).thenThrow(
                new IllegalStateException());
        final ApiProvider provider = mockApiProvider(oauth);
        final InstallData d = ZonkySettingsValidatorTest.mockInstallData();
        // execute SUT
        final ZonkySettingsValidator validator = new ZonkySettingsValidator(() -> provider);
        final DataValidator.Status result = validator.validateData(d);
        // test
        assertThat(result).isEqualTo(DataValidator.Status.WARNING);
    }

    @Test
    void error() {
        // mock data
        final OAuth oauth = mock(OAuth.class);
        when(oauth.login(any(), any()))
                .thenThrow(new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR));
        final ApiProvider provider = mockApiProvider(oauth);
        final InstallData d = ZonkySettingsValidatorTest.mockInstallData();
        // execute SUT
        final ZonkySettingsValidator validator = new ZonkySettingsValidator(() -> provider);
        final DataValidator.Status result = validator.validateData(d);
        // test
        assertThat(result).isEqualTo(DataValidator.Status.ERROR);
    }
}
