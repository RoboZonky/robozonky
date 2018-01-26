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

package com.github.robozonky.app.configuration;

import java.util.Optional;
import java.util.UUID;

import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.configuration.daemon.DaemonInvestmentMode;
import com.github.robozonky.common.secrets.SecretProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class OperatingModeTest {

    private static final String SERVICE = "zonkoid", SERVICE_TOKEN = "123456";

    @Test
    void withConfirmation() {
        final TweaksCommandLineFragment f = mock(TweaksCommandLineFragment.class);
        when(f.isDryRunEnabled()).thenReturn(true);
        final CommandLine cli = mock(CommandLine.class);
        when(cli.getTweaksFragment()).thenReturn(f);
        final SecretProvider secretProvider = SecretProvider.fallback("user", "pass".toCharArray());
        final Authenticated auth = Authenticated.passwordBased(secretProvider);
        final ConfirmationCommandLineFragment fragment = new ConfirmationCommandLineFragment();
        fragment.confirmationCredentials = SERVICE + ":" + SERVICE_TOKEN;
        when(cli.getConfirmationFragment()).thenReturn(fragment);
        final OperatingMode mode = new DaemonOperatingMode(t -> {
        });
        final Optional<InvestmentMode> config = mode.configure(cli, auth);
        assertSoftly(softly -> {
            softly.assertThat(config).containsInstanceOf(DaemonInvestmentMode.class);
            softly.assertThat(secretProvider.getSecret(SERVICE)).contains(SERVICE_TOKEN.toCharArray());
        });
    }

    @Test
    void withConfirmationAndUnknownId() {
        final CommandLine cli = mock(CommandLine.class);
        when(cli.getTweaksFragment()).thenReturn(mock(TweaksCommandLineFragment.class));
        final SecretProvider secretProvider = SecretProvider.fallback("user", "pass".toCharArray());
        final Authenticated auth = Authenticated.passwordBased(secretProvider);
        final ConfirmationCommandLineFragment fragment = new ConfirmationCommandLineFragment();
        fragment.confirmationCredentials = UUID.randomUUID().toString();
        when(cli.getConfirmationFragment()).thenReturn(fragment);
        final OperatingMode mode = new DaemonOperatingMode(t -> {
        });
        final Optional<InvestmentMode> config = mode.configure(cli, auth);
        assertThat(config).isEmpty();
        assertThat(secretProvider.getSecret(SERVICE)).isEmpty();
    }

    @Test
    void withConfirmationAndNoSecret() {
        final CommandLine cli = mock(CommandLine.class);
        when(cli.getTweaksFragment()).thenReturn(mock(TweaksCommandLineFragment.class));
        final SecretProvider secretProvider = SecretProvider.fallback("user", "pass".toCharArray());
        final Authenticated auth = Authenticated.passwordBased(secretProvider);
        final ConfirmationCommandLineFragment fragment = new ConfirmationCommandLineFragment();
        fragment.confirmationCredentials = SERVICE;
        when(cli.getConfirmationFragment()).thenReturn(fragment);
        final OperatingMode mode = new DaemonOperatingMode(t -> {
        });
        final Optional<InvestmentMode> config = mode.configure(cli, auth);
        assertThat(config).isEmpty();
        assertThat(secretProvider.getSecret(SERVICE)).isEmpty();
    }

    @Test
    void withoutConfirmation() {
        final CommandLine cli = mock(CommandLine.class);
        when(cli.getTweaksFragment()).thenReturn(mock(TweaksCommandLineFragment.class));
        when(cli.getConfirmationFragment()).thenReturn(mock(ConfirmationCommandLineFragment.class));
        final SecretProvider secretProvider = SecretProvider.fallback("user", new char[0]);
        final Authenticated auth = Authenticated.passwordBased(secretProvider);
        final OperatingMode mode = new DaemonOperatingMode(t -> {
        });
        final Optional<InvestmentMode> config = mode.configure(cli, auth);
        assertThat(config).isPresent();
        assertThat(secretProvider.getSecret(SERVICE)).isEmpty();
    }
}
