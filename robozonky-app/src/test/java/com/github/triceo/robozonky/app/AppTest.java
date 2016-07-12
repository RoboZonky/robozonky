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
import java.math.BigDecimal;
import java.nio.file.Files;
import java.security.KeyStoreException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.github.triceo.robozonky.Investor;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Wallet;
import com.github.triceo.robozonky.remote.ZonkyApi;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import static com.github.triceo.robozonky.app.App.processCommandLine;

public class AppTest extends AbstractNonExitingTest {

    @Test
    public void simpleCommandLine() {
        final AppContext ctx = processCommandLine("-s", "src/main/assembly/resources/robozonky-conservative.cfg",
                "-u", "user", "-p", "pass");
        Assertions.assertThat(ctx.getOperatingMode()).isEqualTo(OperatingMode.STRATEGY_DRIVEN);
        Assertions.assertThat(ctx.isDryRun()).isFalse();
        Assertions.assertThat(ctx.getAuthenticationHandler()).isNotNull();
        Assertions.assertThat(ctx.getInvestmentStrategy()).isNotNull();
    }

    @Test(expected = RoboZonkyTestingExitException.class)
    public void unreadableStrategyFile() {
        processCommandLine("-s", "something", "-u", "user", "-p", "pass");
    }

    @Test(expected = RoboZonkyTestingExitException.class)
    public void nothingOnCommandLine() {
        Assertions.assertThat(processCommandLine());
    }

    @Test
    public void storeInvestmentData() throws IOException {
        final Investment mock = Mockito.mock(Investment.class);
        Mockito.when(mock.getLoanId()).thenReturn(1);
        Mockito.when(mock.getAmount()).thenReturn(2);
        final String expectedResult = "#1: 2 CZK";

        final File f = File.createTempFile("robozonky-", ".investments");
        f.delete();
        Assume.assumeFalse(f.exists());

        final Optional<File> result = App.storeInvestmentsMade(f, Collections.singleton(mock));
        Assertions.assertThat(result).contains(f);
        Assertions.assertThat(f).exists();
        Assertions.assertThat(Files.lines(f.toPath())).containsExactly(expectedResult);
    }

    @Test
    public void storeNoInvestmentData() throws IOException {
        Assertions.assertThat(App.storeInvestmentsMade(null, Collections.emptySet())).isEmpty();
    }

    @Test
    public void storeInvestmentDataWithDryRun() throws IOException {
        final Optional<File> result =
                App.storeInvestmentsMade(Collections.singleton(Mockito.mock(Investment.class)), true);
        Assertions.assertThat(result).isPresent();
        final Optional<File> result2 =
                App.storeInvestmentsMade(Collections.singleton(Mockito.mock(Investment.class)), false);
        Assertions.assertThat(result2).isPresent();
        Assertions.assertThat(result2.get().getAbsolutePath()).isNotEqualTo(result.get().getAbsolutePath());
    }

    @Test
    public void properBalanceRetrievalInDryRun() {
        // prepare context
        final BigDecimal dryRunBalance = BigDecimal.valueOf(12345);
        final AppContext ctx = Mockito.mock(AppContext.class);
        Mockito.when(ctx.isDryRun()).thenReturn(true);
        Mockito.when(ctx.getDryRunBalance()).thenReturn(dryRunBalance.intValue());
        // test operation
        Assertions.assertThat(App.getAvailableBalance(ctx, null)).isEqualTo(dryRunBalance);
    }

    @Test
    public void properBalanceRetrievalInNormalMode() {
        // prepare context
        final BigDecimal remoteBalance = BigDecimal.valueOf(12345);
        final Wallet wallet = new Wallet(-1, -1, BigDecimal.valueOf(100000), remoteBalance);
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getWallet()).thenReturn(wallet);
        final AppContext ctx = Mockito.mock(AppContext.class);
        // test operation
        Assertions.assertThat(App.getAvailableBalance(ctx, api)).isEqualTo(remoteBalance);
    }

    private static AppContext mockContext(final OperatingMode mode) {
        final AppContext ctx = Mockito.mock(AppContext.class);
        Mockito.when(ctx.getLoanId()).thenReturn(1);
        Mockito.when(ctx.getLoanAmount()).thenReturn(1000);
        Mockito.when(ctx.getOperatingMode()).thenReturn(mode);
        return ctx;
    }

    @Test
    public void strategyDrivenInvestingFunction() {
        final AppContext ctx = AppTest.mockContext(OperatingMode.STRATEGY_DRIVEN);
        final Investor i = Mockito.mock(Investor.class);
        final Investment investment = Mockito.mock(Investment.class);
        Mockito.when(i.invest()).thenReturn(Collections.singletonList(investment));
        final Collection<Investment> result = App.getInvestingFunction(ctx).apply(i);
        Mockito.verify(i, Mockito.times(1)).invest();
        Assertions.assertThat(result).containsExactly(investment);
    }

    @Test
    public void userDrivenInvestingFunction() {
        final AppContext ctx = AppTest.mockContext(OperatingMode.USER_DRIVEN);
        final Investor i = Mockito.mock(Investor.class);
        // check what happens when nothing is invested)
        Mockito.when(i.invest(Matchers.anyInt(), Matchers.anyInt())).thenReturn(Optional.empty());
        final Collection<Investment> result = App.getInvestingFunction(ctx).apply(i);
        Mockito.verify(i, Mockito.times(1)).invest(ctx.getLoanId(), ctx.getLoanAmount());
        Assertions.assertThat(result).isEmpty();
        // check what happens when something is invested
        final Investment investment = Mockito.mock(Investment.class);
        Mockito.when(i.invest(Matchers.anyInt(), Matchers.anyInt())).thenReturn(Optional.of(investment));
        final Collection<Investment> result2 = App.getInvestingFunction(ctx).apply(i);
        Assertions.assertThat(result2).containsExactly(investment);
    }

    @Test()
    public void wrongFormatKeyStoreProvided() throws IOException {
        final File tmp = File.createTempFile("robozonky-", ".keystore");
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getPassword()).thenReturn("password");
        Mockito.when(cli.getKeyStoreLocation()).thenReturn(Optional.of(tmp));
        App.getSensitiveInformationProvider(cli, null);
        Mockito.verify(cli, Mockito.times(1)).printHelpAndExit(Matchers.any(), Matchers.any(KeyStoreException.class));
    }

}
