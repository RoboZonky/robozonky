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

import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class OperatingModeTest extends AbstractNonExitingTest {

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
        final AuthenticationHandler auth = Mockito.mock(AuthenticationHandler.class);
        final AppContext result = OperatingMode.USER_DRIVEN.setup(cli, auth);
        Assertions.assertThat(result.getLoanId()).isEqualTo(cli.getLoanId().get());
        Assertions.assertThat(result.getLoanAmount()).isEqualTo(cli.getLoanAmount().get());
        Assertions.assertThat(result.getAuthenticationHandler()).isEqualTo(auth);
        Assertions.assertThat(result.isDryRun()).isFalse();
    }

    private void ensureExitCalled(final CommandLineInterface mock) {
        Mockito.verify(mock, Mockito.times(1)).printHelpAndExit(Mockito.any(), Mockito.eq(true));
    }

    private void ensureThrows(final CommandLineInterface mock, final Class<? extends Exception> ex) {
        Mockito.verify(mock, Mockito.times(1)).printHelpAndExit(Mockito.any(), Mockito.any(ex));
    }

    @Test
    public void userDrivenLoanAmountMissing() {
        final CommandLineInterface cli = OperatingModeTest.mockCliNoAmount(1);
        OperatingMode.USER_DRIVEN.setup(cli, Mockito.mock(AuthenticationHandler.class));
        this.ensureExitCalled(cli);
    }

    @Test
    public void userDrivenLoanAmountWrong() {
        final CommandLineInterface cli = OperatingModeTest.mockCli(1, 0);
        OperatingMode.USER_DRIVEN.setup(cli, Mockito.mock(AuthenticationHandler.class));
        this.ensureExitCalled(cli);
    }

    @Test
    public void userDrivenLoanIdMissing() {
        final CommandLineInterface cli = OperatingModeTest.mockCliNoLoanId(1000);
        OperatingMode.USER_DRIVEN.setup(cli, Mockito.mock(AuthenticationHandler.class));
        this.ensureExitCalled(cli);
    }

    @Test
    public void userDrivenLoanIdWrong() {
        final CommandLineInterface cli = OperatingModeTest.mockCli(0, 1000);
        OperatingMode.USER_DRIVEN.setup(cli, Mockito.mock(AuthenticationHandler.class));
        this.ensureExitCalled(cli);
    }

    @Test
    public void standardStrategyDriven() {
        final CommandLineInterface cli = OperatingModeTest.mockCli();
        Mockito.when(cli.getLoanId()).thenReturn(Optional.empty());
        Mockito.when(cli.getLoanAmount()).thenReturn(Optional.empty());
        Mockito.when(cli.getStrategyConfigurationFilePath())
                .thenReturn(Optional.of("src/main/assembly/resources/robozonky-dynamic.cfg"));
        final AppContext result = OperatingMode.STRATEGY_DRIVEN.setup(cli, Mockito.mock(AuthenticationHandler.class));
        Assertions.assertThat(result.getInvestmentStrategy()).isNotNull();
        Assertions.assertThat(result.isDryRun()).isFalse();
    }

    @Test
    public void strategyDrivenNoFile() {
        final CommandLineInterface cli = OperatingModeTest.mockCli();
        Mockito.when(cli.getStrategyConfigurationFilePath()).thenReturn(Optional.empty());
        OperatingMode.STRATEGY_DRIVEN.setup(cli, Mockito.mock(AuthenticationHandler.class));
        this.ensureExitCalled(cli);
    }

    @Test
    public void strategyDrivenWrongFile() throws IOException {
        final CommandLineInterface cli = OperatingModeTest.mockCli();
        final File tmp = File.createTempFile("robozonky-", ".strategy"); // this is an empty (= invalid) strategy file
        Mockito.when(cli.getStrategyConfigurationFilePath()).thenReturn(Optional.of(tmp.getAbsolutePath()));
        OperatingMode.STRATEGY_DRIVEN.setup(cli, Mockito.mock(AuthenticationHandler.class));
        this.ensureThrows(cli, IllegalStateException.class);
    }
}

