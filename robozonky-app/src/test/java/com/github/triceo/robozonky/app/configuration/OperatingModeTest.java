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

package com.github.triceo.robozonky.app.configuration;

import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import com.github.triceo.robozonky.ZonkyProxy;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.authentication.SecretProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class OperatingModeTest {

    @Test
    public void defaultDirect() {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getTweaksFragment()).thenReturn(Mockito.mock(TweaksCommandLineFragment.class));
        final ZonkyProxy.Builder builder = new ZonkyProxy.Builder();
        final AuthenticationHandler auth =
                AuthenticationHandler.passwordBased(SecretProvider.fallback("user", "pass".toCharArray()));
        final OperatingMode mode = OperatingMode.DIRECT_INVESTMENT;
        final Configuration config = mode.getConfiguration(cli, auth, builder);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mode.getName()).isEqualTo("single");
            softly.assertThat(config.getZonkyProxyBuilder()).isEqualTo(builder);
        });
    }

    @Test
    public void defaultStrategy() {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getTweaksFragment()).thenReturn(Mockito.mock(TweaksCommandLineFragment.class));
        final ZonkyProxy.Builder builder = new ZonkyProxy.Builder();
        final AuthenticationHandler auth =
                AuthenticationHandler.passwordBased(SecretProvider.fallback("user", "pass".toCharArray()));
        final OperatingMode mode = OperatingMode.STRATEGY_BASED;
        final Configuration config = mode.getConfiguration(cli, auth, builder);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mode.getName()).isEqualTo("many");
            softly.assertThat(config.getInvestmentStrategy()).isPresent();
            softly.assertThat(config.getZonkyProxyBuilder()).isEqualTo(builder);
            softly.assertThat(config.getSleepPeriod().get(ChronoUnit.SECONDS)).isEqualTo(60 * 60);
        });
    }

    @Test
    public void withConfirmation() {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getTweaksFragment()).thenReturn(Mockito.mock(TweaksCommandLineFragment.class));
        final AuthenticationHandler auth =
                AuthenticationHandler.passwordBased(SecretProvider.fallback("user", "pass".toCharArray()));
        final ConfirmationCommandLineFragment fragment = new ConfirmationCommandLineFragment();
        fragment.confirmationCredentials = "zonkoid:123456";
        Mockito.when(cli.getConfirmationFragment()).thenReturn(fragment);
        final OperatingMode mode = OperatingMode.DIRECT_INVESTMENT;
        final Optional<Configuration> config = mode.configure(cli, auth);
        Assertions.assertThat(config).isPresent();
        final ZonkyProxy proxy = config.get().getZonkyProxyBuilder().build(Mockito.mock(ZonkyApi.class));
        Assertions.assertThat(proxy.getConfirmationProviderId()).isPresent();
    }

    @Test
    public void withConfirmationAndUnknownId() {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getTweaksFragment()).thenReturn(Mockito.mock(TweaksCommandLineFragment.class));
        final AuthenticationHandler auth =
                AuthenticationHandler.passwordBased(SecretProvider.fallback("user", "pass".toCharArray()));
        final ConfirmationCommandLineFragment fragment = new ConfirmationCommandLineFragment();
        fragment.confirmationCredentials = UUID.randomUUID().toString();
        Mockito.when(cli.getConfirmationFragment()).thenReturn(fragment);
        final OperatingMode mode = OperatingMode.DIRECT_INVESTMENT;
        final Optional<Configuration> config = mode.configure(cli, auth);
        Assertions.assertThat(config).isEmpty();
    }

    @Test
    public void withConfirmationAndNoSecret() {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getTweaksFragment()).thenReturn(Mockito.mock(TweaksCommandLineFragment.class));
        final AuthenticationHandler auth =
                AuthenticationHandler.passwordBased(SecretProvider.fallback("user", "pass".toCharArray()));
        final ConfirmationCommandLineFragment fragment = new ConfirmationCommandLineFragment();
        fragment.confirmationCredentials = "zonkoid";
        Mockito.when(cli.getConfirmationFragment()).thenReturn(fragment);
        final OperatingMode mode = OperatingMode.DIRECT_INVESTMENT;
        final Optional<Configuration> config = mode.configure(cli, auth);
        Assertions.assertThat(config).isEmpty();
    }

    @Test
    public void withoutConfirmation() {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getTweaksFragment()).thenReturn(Mockito.mock(TweaksCommandLineFragment.class));
        Mockito.when(cli.getConfirmationFragment()).thenReturn(Mockito.mock(ConfirmationCommandLineFragment.class));
        final OperatingMode mode = OperatingMode.DIRECT_INVESTMENT;
        final Optional<Configuration> config = mode.configure(cli, null);
        Assertions.assertThat(config).isPresent();
        final ZonkyProxy proxy = config.get().getZonkyProxyBuilder().build(Mockito.mock(ZonkyApi.class));
        Assertions.assertThat(proxy.getConfirmationProviderId()).isEmpty();
    }

}
