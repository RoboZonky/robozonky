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

package com.github.triceo.robozonky.app.configuration;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class CommandLineInterfaceTest {

    private static CommandLineInterface process(final Optional<CommandLineInterface> maybe) {
        Assertions.assertThat(maybe).isPresent();
        return maybe.get();
    }

    @Test
    public void failingCli() {
        final Optional<CommandLineInterface> cli = CommandLineInterface.parse();
        Assertions.assertThat(cli).isEmpty();
    }

    @Test
    public void minimalStrategyDrivenCliWithKeyStoreAndToken() {
        final CommandLineInterface cli = CommandLineInterfaceTest.process(
                CommandLineInterface.parse("-s", "path", "-g", "key", "-p", "pwd", "-r", "5"));
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cli.getStrategyConfigurationFilePath()).isPresent();
        softly.assertThat(cli.getKeyStoreLocation()).isPresent();
        softly.assertThat(cli.isTokenEnabled()).isTrue();
        softly.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isPresent();
        softly.assertThat(cli.getPassword()).isNotNull();
        softly.assertThat(cli.getUsername()).isEmpty();
        softly.assertAll();
    }

    @Test
    public void minimalFaultTolerantStrategyDrivenCliWithKeyStoreAndTokenNoRefresh() {
        final CommandLineInterface cli = CommandLineInterfaceTest.process(
                CommandLineInterface.parse("-s", "path", "-g", "key", "-p", "pwd", "-r", "-t"));
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cli.getStrategyConfigurationFilePath()).isPresent();
        softly.assertThat(cli.getKeyStoreLocation()).isPresent();
        softly.assertThat(cli.isTokenEnabled()).isTrue();
        softly.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isEmpty();
        softly.assertThat(cli.getPassword()).isNotNull();
        softly.assertThat(cli.getUsername()).isEmpty();
        softly.assertThat(cli.isFaultTolerant()).isTrue();
        softly.assertAll();
    }

    @Test
    public void minimalStrategyDrivenCli() {
        final CommandLineInterface cli = CommandLineInterfaceTest.process(
                CommandLineInterface.parse("-s", "somePath", "-u", "user", "-p", "password"));
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cli.getStrategyConfigurationFilePath()).isPresent();
        softly.assertThat(cli.getUsername()).isPresent();
        softly.assertThat(cli.getPassword()).isNotNull();
        softly.assertThat(cli.getLoanId()).isEmpty();
        softly.assertThat(cli.getLoanAmount()).isEmpty();
        softly.assertThat(cli.isDryRun()).isFalse();
        softly.assertThat(cli.getDryRunBalance()).isEmpty();
        softly.assertThat(cli.getKeyStoreLocation()).isEmpty();
        softly.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isEmpty();
        softly.assertThat(cli.isFaultTolerant()).isFalse();
        softly.assertAll();
    }

    @Test
    public void minimalUserDrivenCli() {
        final CommandLineInterface cli = CommandLineInterfaceTest.process(
                        CommandLineInterface.parse("-l", "1", "-a", "1000", "-u", "user", "-p", "pwd"));
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cli.getLoanId()).isPresent();
        softly.assertThat(cli.getLoanAmount()).isPresent();
        softly.assertThat(cli.getUsername()).isPresent();
        softly.assertThat(cli.getPassword()).isNotNull();
        softly.assertThat(cli.getStrategyConfigurationFilePath()).isEmpty();
        softly.assertThat(cli.isDryRun()).isFalse();
        softly.assertThat(cli.getDryRunBalance()).isEmpty();
        softly.assertThat(cli.getKeyStoreLocation()).isEmpty();
        softly.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isEmpty();
        softly.assertThat(cli.isTokenEnabled()).isFalse();
        softly.assertThat(cli.isFaultTolerant()).isFalse();
        softly.assertAll();
    }

    @Test
    public void userDrivenCliWrongAmount() {
        final CommandLineInterface cli = CommandLineInterfaceTest.process(
                CommandLineInterface.parse("-l", "1", "-a", "10a0", "-u", "user", "-p", "pwd"));
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cli.getLoanId()).isPresent();
        softly.assertThat(cli.getUsername()).isPresent();
        softly.assertThat(cli.getLoanAmount()).isEmpty();
        softly.assertThat(cli.getPassword()).isNotNull();
        softly.assertThat(cli.getStrategyConfigurationFilePath()).isEmpty();
        softly.assertThat(cli.isDryRun()).isFalse();
        softly.assertThat(cli.getDryRunBalance()).isEmpty();
        softly.assertThat(cli.getKeyStoreLocation()).isEmpty();
        softly.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isEmpty();
        softly.assertThat(cli.isTokenEnabled()).isFalse();
        softly.assertAll();
    }

    @Test
    public void properScriptIdentification() {
        System.setProperty("os.name", "Some Windows System");
        Assertions.assertThat(CommandLineInterface.getScriptIdentifier()).isEqualTo("robozonky.bat");
        System.setProperty("os.name", "Any Other System");
        Assertions.assertThat(CommandLineInterface.getScriptIdentifier()).isEqualTo("robozonky.sh");
    }

}
