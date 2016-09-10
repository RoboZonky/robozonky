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

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.github.triceo.robozonky.util.IoTestUtil;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assume;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class OperatingModeTest {

    private static CommandLineInterface mockCli() {
        return OperatingModeTest.mockCli(Optional.empty(), Optional.empty());
    }

    private static CommandLineInterface mockCliNoAmount(final Integer loanId) {
        return OperatingModeTest.mockCli(Optional.of(loanId), Optional.empty());
    }

    private static CommandLineInterface mockCliNoLoanId(final Integer loanAmount) {
        return OperatingModeTest.mockCli(Optional.empty(), Optional.of(loanAmount));
    }

    private static CommandLineInterface mockCli(final Integer loanId, final Integer loanAmount) {
        return OperatingModeTest.mockCli(Optional.of(loanId), Optional.of(loanAmount));
    }

    private static CommandLineInterface mockCli(final Optional<Integer> loanId, final Optional<Integer> loanAmount) {
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getLoanAmount()).thenReturn(loanId);
        Mockito.when(cli.getLoanId()).thenReturn(loanAmount);
        return cli;
    }

    @Test
    public void standardUserDriven() {
        final CommandLineInterface cli = OperatingModeTest.mockCli(1000, 1);
        final Optional<AppContext> optionalResult = OperatingMode.USER_DRIVEN.setup(cli);
        Assertions.assertThat(optionalResult).isPresent();
        final AppContext result = optionalResult.get();
        Assertions.assertThat(result.getLoanId()).isEqualTo(cli.getLoanId().get());
        Assertions.assertThat(result.getLoanAmount()).isEqualTo(cli.getLoanAmount().get());
        Assertions.assertThat(result.isDryRun()).isFalse();
    }

    @Test
    public void standardUserDrivenDryRun() {
        final int balance = 100;
        final CommandLineInterface cli = OperatingModeTest.mockCli(1000, 1);
        Mockito.when(cli.isDryRun()).thenReturn(true);
        Mockito.when(cli.getDryRunBalance()).thenReturn(Optional.of(balance));
        final Optional<AppContext> optionalResult = OperatingMode.USER_DRIVEN.setup(cli);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(optionalResult).isPresent();
        final AppContext result = optionalResult.get();
        softly.assertThat(result.getLoanId()).isEqualTo(cli.getLoanId().get());
        softly.assertThat(result.getLoanAmount()).isEqualTo(cli.getLoanAmount().get());
        softly.assertThat(result.isDryRun()).isTrue();
        softly.assertThat(result.getDryRunBalance()).isEqualTo(100);
        softly.assertAll();
    }

    private static void ensureHelpCalled(final CommandLineInterface mock) {
        Mockito.verify(mock, Mockito.times(1)).printHelp(Matchers.any(), Matchers.eq(true));
    }

    @Test
    public void userDrivenLoanAmountMissing() {
        final CommandLineInterface cli = OperatingModeTest.mockCliNoAmount(1);
        Assertions.assertThat(OperatingMode.USER_DRIVEN.setup(cli))
                .isEmpty();
        OperatingModeTest.ensureHelpCalled(cli);
    }

    @Test
    public void userDrivenLoanAmountWrong() {
        final CommandLineInterface cli = OperatingModeTest.mockCli(1, 0);
        Assertions.assertThat(OperatingMode.USER_DRIVEN.setup(cli))
                .isEmpty();
        OperatingModeTest.ensureHelpCalled(cli);
    }

    @Test
    public void userDrivenLoanDataMissing() {
        final CommandLineInterface cli = OperatingModeTest.mockCli();
        Assertions.assertThat(OperatingMode.USER_DRIVEN.setup(cli))
                .isEmpty();
        OperatingModeTest.ensureHelpCalled(cli);
    }

    @Test
    public void userDrivenLoanIdMissing() {
        final CommandLineInterface cli = OperatingModeTest.mockCliNoLoanId(1000);
        Assertions.assertThat(OperatingMode.USER_DRIVEN.setup(cli))
                .isEmpty();
        OperatingModeTest.ensureHelpCalled(cli);
    }

    @Test
    public void userDrivenLoanIdWrong() {
        final CommandLineInterface cli = OperatingModeTest.mockCli(0, 1000);
        Assertions.assertThat(OperatingMode.USER_DRIVEN.setup(cli))
                .isEmpty();
        OperatingModeTest.ensureHelpCalled(cli);
    }

    @Test
    public void standardStrategyDriven() {
        final CommandLineInterface cli = OperatingModeTest.mockCli();
        Mockito.when(cli.getLoanId()).thenReturn(Optional.empty());
        Mockito.when(cli.getLoanAmount()).thenReturn(Optional.empty());
        Mockito.when(cli.getStrategyConfigurationFilePath())
                .thenReturn(Optional.of(IoTestUtil.findMainSource("assembly", "resources", "robozonky-dynamic.cfg")));
        final Optional<AppContext> optionalResult = OperatingMode.STRATEGY_DRIVEN.setup(cli);
        Assertions.assertThat(optionalResult).isPresent();
        final AppContext result = optionalResult.get();
        Assertions.assertThat(result.getInvestmentStrategy()).isNotNull();
        Assertions.assertThat(result.isDryRun()).isFalse();
    }

    @Test
    public void strategyDrivenNoFile() {
        final CommandLineInterface cli = OperatingModeTest.mockCli();
        Mockito.when(cli.getStrategyConfigurationFilePath()).thenReturn(Optional.empty());
        final Optional<AppContext> result =
            OperatingMode.STRATEGY_DRIVEN.setup(cli);
        Assertions.assertThat(result).isEmpty();
        OperatingModeTest.ensureHelpCalled(cli);
    }

    @Test
    public void strategyDrivenLoanGiven() {
        final CommandLineInterface cli = OperatingModeTest.mockCli(1, 2);
        Mockito.when(cli.getStrategyConfigurationFilePath()).thenReturn(Optional.empty());
        final Optional<AppContext> result =
                OperatingMode.STRATEGY_DRIVEN.setup(cli);
        Assertions.assertThat(result).isEmpty();
        OperatingModeTest.ensureHelpCalled(cli);
    }

    @Test
    public void strategyDrivenWrongFile() throws IOException {
        final CommandLineInterface cli = OperatingModeTest.mockCli();
        final File tmp = File.createTempFile("robozonky-", ".strategy"); // this is an empty (= invalid) strategy file
        Mockito.when(cli.getStrategyConfigurationFilePath()).thenReturn(Optional.of(tmp.getAbsolutePath()));
        Assertions.assertThat(OperatingMode.STRATEGY_DRIVEN.setup(cli))
                .isEmpty();
    }

    @Test
    public void strategyDrivenNonExistentFile() throws IOException {
        final CommandLineInterface cli = OperatingModeTest.mockCli();
        final File tmp = File.createTempFile("robozonky-", ".strategy"); // this is an empty (= invalid) strategy file
        Assume.assumeTrue(tmp.delete());
        Mockito.when(cli.getStrategyConfigurationFilePath()).thenReturn(Optional.of(tmp.getAbsolutePath()));
        Assertions.assertThat(OperatingMode.STRATEGY_DRIVEN.setup(cli))
                .isEmpty();
        OperatingModeTest.ensureHelpCalled(cli);
    }
}

