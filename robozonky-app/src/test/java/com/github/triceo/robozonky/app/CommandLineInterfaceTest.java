/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CommandLineInterfaceTest extends AbstractNonExitingTest {

    @Test(expected = RoboZonkyTestingExitException.class)
    public void noArguments() {
        CommandLineInterface.parse();
    }

    @Test
    public void minimalStrategyDrivenCliWithKeyStoreAndToken() {
        final CommandLineInterface cli = CommandLineInterface.parse("-s", "path", "-g", "key", "-p", "pwd", "-r", "5");
        Assertions.assertThat(cli.getCliOperatingMode()).isEqualTo(Optional.of(OperatingMode.STRATEGY_DRIVEN));
        Assertions.assertThat(cli.getStrategyConfigurationFilePath()).isPresent();
        Assertions.assertThat(cli.getKeyStoreLocation()).isPresent();
        Assertions.assertThat(cli.isTokenEnabled()).isTrue();
        Assertions.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isPresent();
        Assertions.assertThat(cli.getPassword()).isNotNull();
        Assertions.assertThat(cli.getUsername()).isEmpty();
    }

    @Test
    public void minimalStrategyDrivenCliWithKeyStoreAndTokenNoRefresh() {
        final CommandLineInterface cli = CommandLineInterface.parse("-s", "path", "-g", "key", "-p", "pwd", "-r");
        Assertions.assertThat(cli.getCliOperatingMode()).isEqualTo(Optional.of(OperatingMode.STRATEGY_DRIVEN));
        Assertions.assertThat(cli.getStrategyConfigurationFilePath()).isPresent();
        Assertions.assertThat(cli.getKeyStoreLocation()).isPresent();
        Assertions.assertThat(cli.isTokenEnabled()).isTrue();
        Assertions.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isEmpty();
        Assertions.assertThat(cli.getPassword()).isNotNull();
        Assertions.assertThat(cli.getUsername()).isEmpty();
    }

    @Test
    public void minimalStrategyDrivenCli() {
        final CommandLineInterface cli = CommandLineInterface.parse("-s", "somePath", "-u", "user", "-p", "password");
        Assertions.assertThat(cli.getCliOperatingMode()).isEqualTo(Optional.of(OperatingMode.STRATEGY_DRIVEN));
        Assertions.assertThat(cli.getStrategyConfigurationFilePath()).isPresent();
        Assertions.assertThat(cli.getUsername()).isPresent();
        Assertions.assertThat(cli.getPassword()).isNotNull();
        Assertions.assertThat(cli.getLoanId()).isEmpty();
        Assertions.assertThat(cli.getLoanAmount()).isEmpty();
        Assertions.assertThat(cli.isDryRun()).isFalse();
        Assertions.assertThat(cli.getDryRunBalance()).isEmpty();
        Assertions.assertThat(cli.getKeyStoreLocation()).isEmpty();
        Assertions.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isEmpty();
    }

    @Test
    public void minimalUserDrivenCli() {
        final CommandLineInterface cli = CommandLineInterface.parse("-l", "1", "-a", "1000", "-u", "user", "-p", "pwd");
        Assertions.assertThat(cli.getCliOperatingMode()).isEqualTo(Optional.of(OperatingMode.USER_DRIVEN));
        Assertions.assertThat(cli.getLoanId()).isPresent();
        Assertions.assertThat(cli.getLoanAmount()).isPresent();
        Assertions.assertThat(cli.getUsername()).isPresent();
        Assertions.assertThat(cli.getPassword()).isNotNull();
        Assertions.assertThat(cli.getStrategyConfigurationFilePath()).isEmpty();
        Assertions.assertThat(cli.isDryRun()).isFalse();
        Assertions.assertThat(cli.getDryRunBalance()).isEmpty();
        Assertions.assertThat(cli.getKeyStoreLocation()).isEmpty();
        Assertions.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isEmpty();
        Assertions.assertThat(cli.isTokenEnabled()).isFalse();
    }

    @Test
    public void userDrivenCliWrongAmount() {
        final CommandLineInterface cli = CommandLineInterface.parse("-l", "1", "-a", "10a0", "-u", "user", "-p", "pwd");
        Assertions.assertThat(cli.getCliOperatingMode()).isEqualTo(Optional.of(OperatingMode.USER_DRIVEN));
        Assertions.assertThat(cli.getLoanId()).isPresent();
        Assertions.assertThat(cli.getUsername()).isPresent();
        Assertions.assertThat(cli.getLoanAmount()).isEmpty();
        Assertions.assertThat(cli.getPassword()).isNotNull();
        Assertions.assertThat(cli.getStrategyConfigurationFilePath()).isEmpty();
        Assertions.assertThat(cli.isDryRun()).isFalse();
        Assertions.assertThat(cli.getDryRunBalance()).isEmpty();
        Assertions.assertThat(cli.getKeyStoreLocation()).isEmpty();
        Assertions.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isEmpty();
        Assertions.assertThat(cli.isTokenEnabled()).isFalse();
    }

}
