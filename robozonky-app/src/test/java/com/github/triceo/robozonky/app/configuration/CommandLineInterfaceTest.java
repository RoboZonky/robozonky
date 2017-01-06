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

import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemOutRule;

public class CommandLineInterfaceTest {

    private static CommandLineInterface process(final Optional<CommandLineInterface> maybe) {
        Assertions.assertThat(maybe).isPresent();
        return maybe.get();
    }

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Before
    public void clearLog() {
        systemOutRule.clearLog();
        systemOutRule.muteForSuccessfulTests();
    }

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Test
    public void failingCli() {
        final Optional<CommandLineInterface> cli = CommandLineInterface.parse();
        Assertions.assertThat(cli).isEmpty();
    }

    @Test
    public void strategyDrivenCliWithKeyStoreAndTokenAndConfirmation() {
        final CommandLineInterface cli = CommandLineInterfaceTest.process(
                CommandLineInterface.parse("-s", "path", "-g", "key", "-p", "pwd", "-r", "5", "-x", "zonkoid:apitest"));
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cli.getStrategyConfigurationLocation()).isPresent();
        softly.assertThat(cli.getKeyStoreLocation()).isPresent();
        softly.assertThat(cli.isTokenEnabled()).isTrue();
        softly.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isPresent().hasValue(5);
        softly.assertThat(cli.getPassword()).isNotNull();
        softly.assertThat(cli.getUsername()).isEmpty();
        softly.assertThat(cli.getCaptchaPreventingInvestingDelayInSeconds())
                .isEqualTo(CommandLineInterface.DEFAULT_CAPTCHA_DELAY_SECONDS);
        softly.assertThat(cli.getMaximumSleepPeriodInMinutes())
                .isEqualTo(CommandLineInterface.DEFAULT_SLEEP_PERIOD_MINUTES);
        softly.assertThat(cli.getConfirmationCredentials()).isPresent();
        softly.assertAll();
    }

    @Test
    public void minimalFaultTolerantStrategyDrivenCliWithKeyStoreAndTokenNoRefresh() {
        final CommandLineInterface cli = CommandLineInterfaceTest.process(
                CommandLineInterface.parse("-s", "path", "-g", "key", "-p", "pwd", "-r", "-t"));
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cli.getStrategyConfigurationLocation()).isPresent();
        softly.assertThat(cli.getKeyStoreLocation()).isPresent();
        softly.assertThat(cli.isTokenEnabled()).isTrue();
        softly.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isEmpty();
        softly.assertThat(cli.getPassword()).isNotNull();
        softly.assertThat(cli.getUsername()).isEmpty();
        softly.assertThat(cli.isFaultTolerant()).isTrue();
        softly.assertThat(cli.newAuthenticationHandler()).isEmpty();
        softly.assertThat(cli.getConfirmationCredentials()).isEmpty();
        softly.assertAll();
    }

    @Test
    public void minimalStrategyDrivenCli() {
        final CommandLineInterface cli = CommandLineInterfaceTest.process(
                CommandLineInterface.parse("-s", "somePath", "-u", "user", "-p", "password"));
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cli.getStrategyConfigurationLocation()).isPresent();
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
        softly.assertThat(cli.getStrategyConfigurationLocation()).isEmpty();
        softly.assertThat(cli.isDryRun()).isFalse();
        softly.assertThat(cli.getDryRunBalance()).isEmpty();
        softly.assertThat(cli.getKeyStoreLocation()).isEmpty();
        softly.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isEmpty();
        softly.assertThat(cli.isTokenEnabled()).isFalse();
        softly.assertThat(cli.isFaultTolerant()).isFalse();
        softly.assertAll();
    }

    @Test
    public void printHelp() {
        final CommandLineInterface cli = CommandLineInterfaceTest.process(
                CommandLineInterface.parse("-l", "1", "-a", "1000", "-u", "user", "-p", "pwd"));
        final String msg = UUID.randomUUID().toString();
        cli.printHelp(msg, false);
        Assertions.assertThat(this.systemOutRule.getLog()).contains(msg);
        this.systemOutRule.clearLog();
        cli.printHelp(msg, true);
        Assertions.assertThat(this.systemOutRule.getLog()).contains("Error: " + msg);
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
        softly.assertThat(cli.getStrategyConfigurationLocation()).isEmpty();
        softly.assertThat(cli.isDryRun()).isFalse();
        softly.assertThat(cli.getDryRunBalance()).isEmpty();
        softly.assertThat(cli.getKeyStoreLocation()).isEmpty();
        softly.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isEmpty();
        softly.assertThat(cli.isTokenEnabled()).isFalse();
        softly.assertAll();
    }

    @Test
    public void properUserDrivenTokenBasedCli() {
        final CommandLineInterface cli = CommandLineInterfaceTest.process(
                CommandLineInterface.parse("-l", "1", "-a", "1000", "-u", "user", "-p", "pwd", "-r"));
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cli.getLoanId()).isPresent();
        softly.assertThat(cli.getUsername()).isPresent();
        softly.assertThat(cli.getLoanAmount()).isPresent().hasValue(1000);
        softly.assertThat(cli.getPassword()).isNotNull();
        softly.assertThat(cli.getStrategyConfigurationLocation()).isEmpty();
        softly.assertThat(cli.isDryRun()).isFalse();
        softly.assertThat(cli.getDryRunBalance()).isEmpty();
        softly.assertThat(cli.getKeyStoreLocation()).isEmpty();
        softly.assertThat(cli.getTokenRefreshBeforeExpirationInSeconds()).isEmpty();
        softly.assertThat(cli.isTokenEnabled()).isTrue();
        softly.assertThat(cli.newAuthenticationHandler()).isPresent();
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
