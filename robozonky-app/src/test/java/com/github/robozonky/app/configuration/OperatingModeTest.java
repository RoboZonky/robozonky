/*
 * Copyright 2018 The RoboZonky Project
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

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.daemon.DaemonInvestmentMode;
import com.github.robozonky.common.secrets.SecretProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperatingModeTest extends AbstractZonkyLeveragingTest {

    private static final String SERVICE = "zonkoid", SERVICE_TOKEN = "123456";

    @Test
    void withConfirmation() {
        final CommandLine cli = mock(CommandLine.class);
        when(cli.isDryRunEnabled()).thenReturn(true);
        when(cli.getStrategyLocation()).thenReturn("");
        final SecretProvider secretProvider = SecretProvider.inMemory("user", "pass".toCharArray());
        when(cli.getConfirmationCredentials()).thenReturn(Optional.of(SERVICE + ":" + SERVICE_TOKEN));
        final OperatingMode mode = new OperatingMode(t -> {
        });
        final Optional<InvestmentMode> config = mode.configure(cli, secretProvider);
        assertSoftly(softly -> {
            softly.assertThat(config).containsInstanceOf(DaemonInvestmentMode.class);
            softly.assertThat(secretProvider.getSecret(SERVICE)).contains(SERVICE_TOKEN.toCharArray());
        });
    }

    @Test
    void withConfirmationAndUnknownId() {
        final CommandLine cli = mock(CommandLine.class);
        final SecretProvider secretProvider = SecretProvider.inMemory("user", "pass".toCharArray());
        when(cli.getConfirmationCredentials()).thenReturn(Optional.of(UUID.randomUUID().toString()));
        final OperatingMode mode = new OperatingMode(t -> {
        });
        final Optional<InvestmentMode> config = mode.configure(cli, secretProvider);
        assertThat(config).isEmpty();
        assertThat(secretProvider.getSecret(SERVICE)).isEmpty();
    }

    @Test
    void withConfirmationAndNoSecret() {
        final CommandLine cli = mock(CommandLine.class);
        final SecretProvider secretProvider = SecretProvider.inMemory("user", "pass".toCharArray());
        when(cli.getConfirmationCredentials()).thenReturn(Optional.of(SERVICE));
        final OperatingMode mode = new OperatingMode(t -> {
        });
        final Optional<InvestmentMode> config = mode.configure(cli, secretProvider);
        assertThat(config).isEmpty();
        assertThat(secretProvider.getSecret(SERVICE)).isEmpty();
    }

    @Test
    void withoutConfirmation() {
        final CommandLine cli = mock(CommandLine.class);
        when(cli.getStrategyLocation()).thenReturn("");
        final SecretProvider secretProvider = SecretProvider.inMemory("user", new char[0]);
        final OperatingMode mode = new OperatingMode(t -> {
        });
        final Optional<InvestmentMode> config = mode.configure(cli, secretProvider);
        assertThat(config).isPresent();
        assertThat(secretProvider.getSecret(SERVICE)).isEmpty();
    }
}
