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
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.OptionalInt;

import com.github.triceo.robozonky.Investor;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.configuration.Configuration;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Wallet;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZotifyApi;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Test;
import org.mockito.Mockito;

public class RemoteTest extends BaseMarketplaceTest {

    @Test
    public void storeInvestmentData() throws IOException {
        final Investment mock = Mockito.mock(Investment.class);
        Mockito.when(mock.getLoanId()).thenReturn(1);
        Mockito.when(mock.getAmount()).thenReturn(2);
        final String expectedResult = "#1: 2 CZK";

        final File f = File.createTempFile("robozonky-", ".investments");
        f.delete();
        Assume.assumeFalse(f.exists());

        final Optional<File> result = Remote.storeInvestmentsMade(f, Collections.singleton(mock));
        Assertions.assertThat(result).contains(f);
        Assertions.assertThat(f).exists();
        Assertions.assertThat(Files.lines(f.toPath())).containsExactly(expectedResult);
    }

    @Test
    public void storeNoInvestmentData() throws IOException {
        Assertions.assertThat(Remote.storeInvestmentsMade(null, Collections.emptySet())).isEmpty();
    }

    @Test
    public void storeInvestmentDataWithDryRun() throws IOException {
        final Optional<File> result =
                Remote.storeInvestmentsMade(Collections.singleton(Mockito.mock(Investment.class)), true);
        Assertions.assertThat(result).isPresent();
        final Optional<File> result2 =
                Remote.storeInvestmentsMade(Collections.singleton(Mockito.mock(Investment.class)), false);
        Assertions.assertThat(result2).isPresent();
        Assertions.assertThat(result2.get().getAbsolutePath()).isNotEqualTo(result.get().getAbsolutePath());
    }

    @Test
    public void properBalanceRetrievalInDryRun() {
        // prepare context
        final BigDecimal dryRunBalance = BigDecimal.valueOf(12345);
        final Configuration ctx = Mockito.mock(Configuration.class);
        Mockito.when(ctx.isDryRun()).thenReturn(true);
        Mockito.when(ctx.getDryRunBalance()).thenReturn(OptionalInt.of(dryRunBalance.intValue()));
        // test operation
        Assertions.assertThat(Remote.getAvailableBalance(ctx, null)).isEqualTo(dryRunBalance);
    }

    @Test
    public void properBalanceRetrievalInNormalMode() {
        // prepare context
        final BigDecimal remoteBalance = BigDecimal.valueOf(12345);
        final Wallet wallet = new Wallet(-1, -1, BigDecimal.valueOf(100000), remoteBalance);
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getWallet()).thenReturn(wallet);
        final Configuration ctx = Mockito.mock(Configuration.class);
        // test operation
        Assertions.assertThat(Remote.getAvailableBalance(ctx, api)).isEqualTo(remoteBalance);
    }

    private static Configuration mockContext(final boolean usesStrategy) {
        final Configuration ctx = Mockito.mock(Configuration.class);
        Mockito.when(ctx.getLoanId()).thenReturn(OptionalInt.of(1));
        Mockito.when(ctx.getLoanAmount()).thenReturn(OptionalInt.of(1000));
        if (usesStrategy) {
            Mockito.when(ctx.getInvestmentStrategy()).thenReturn(Optional.of(Mockito.mock(InvestmentStrategy.class)));
        } else {
            Mockito.when(ctx.getInvestmentStrategy()).thenReturn(Optional.empty());
        }
        Mockito.when(ctx.getDryRunBalance()).thenReturn(OptionalInt.of(10000));
        return ctx;
    }

    @Test
    public void properLoanDelay() {
        final int delayInSeconds = 120;
        final Loan loanOldEnough = Mockito.mock(Loan.class);
        Mockito.when(loanOldEnough.getRemainingInvestment()).thenReturn(1000.0);
        Mockito.when(loanOldEnough.getDatePublished())
                .thenReturn(OffsetDateTime.now().minus(delayInSeconds + 1, ChronoUnit.SECONDS));
        final Loan loanOldExactly = Mockito.mock(Loan.class);
        Mockito.when(loanOldExactly.getRemainingInvestment()).thenReturn(200.0);
        Mockito.when(loanOldExactly.getDatePublished())
                .thenReturn(OffsetDateTime.now().minus(delayInSeconds, ChronoUnit.SECONDS));
        final Loan youngLoan = Mockito.mock(Loan.class);
        Mockito.when(youngLoan.getRemainingInvestment()).thenReturn(600.0);
        Mockito.when(youngLoan.getDatePublished())
                .thenReturn(OffsetDateTime.now().minus(delayInSeconds - 1, ChronoUnit.SECONDS));
        final ZotifyApi apiMock = Mockito.mock(ZotifyApi.class);
        Mockito.when(apiMock.getLoans()).thenReturn(Arrays.asList(loanOldEnough, loanOldExactly, youngLoan));
        final Configuration ctx = Mockito.mock(Configuration.class);
        Mockito.when(ctx.getCaptchaDelayInSeconds()).thenReturn(delayInSeconds);
        Mockito.when(ctx.getSleepPeriodInMinutes()).thenReturn(60);
        final Activity activity = new Activity(ctx, apiMock, Remote.MARKETPLACE_TIMESTAMP);
        final Collection<Loan> result = Remote.getAvailableLoans(ctx, activity);
        Assertions.assertThat(result).containsOnly(loanOldEnough, loanOldExactly);
    }

    @Test
    public void simpleInvestment() {
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getWallet()).thenReturn(new Wallet(0, 0, BigDecimal.ZERO, BigDecimal.ZERO));
        final Configuration c = Mockito.mock(Configuration.class);
        Mockito.when(c.getLoanId()).thenReturn(OptionalInt.of(1));
        Mockito.when(c.getLoanAmount()).thenReturn(OptionalInt.of(1000));
        final Collection<Investment> result = Remote.invest(c, api, null);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void strategyDrivenInvestingFunction() {
        final Configuration ctx = RemoteTest.mockContext(true);
        final Investor i = Mockito.mock(Investor.class);
        final Investment investment = Mockito.mock(Investment.class);
        Mockito.when(i.invest(Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(investment));
        final Collection<Investment> result = Remote.getInvestingFunction(ctx, null).apply(i);
        Mockito.verify(i, Mockito.times(1)).invest(Mockito.any(), Mockito.any());
        Assertions.assertThat(result).containsExactly(investment);
    }

    @Test
    public void userDrivenInvestingFunction() {
        final Configuration ctx = RemoteTest.mockContext(false);
        final Investor i = Mockito.mock(Investor.class);
        // check what happens when nothing is invested)
        Mockito.when(i.invest(Mockito.anyInt(), Mockito.anyInt())).thenReturn(Optional.empty());
        final Collection<Investment> result = Remote.getInvestingFunction(ctx, null).apply(i);
        Mockito.verify(i, Mockito.times(1)).invest(ctx.getLoanId().getAsInt(), ctx.getLoanAmount().getAsInt());
        Assertions.assertThat(result).isEmpty();
        // check what happens when something is invested
        final Investment investment = Mockito.mock(Investment.class);
        Mockito.when(i.invest(Mockito.anyInt(), Mockito.anyInt())).thenReturn(Optional.of(investment));
        final Collection<Investment> result2 = Remote.getInvestingFunction(ctx, null).apply(i);
        Assertions.assertThat(result2).containsExactly(investment);
    }

    @Test
    public void loginFailOnCredentials() {
        final AuthenticationHandler auth = Mockito.mock(AuthenticationHandler.class);
        Mockito.when(auth.execute(Mockito.any(), Mockito.any())).thenReturn(Optional.empty());
        final Configuration ctx = Mockito.mock(Configuration.class);
        Assertions.assertThat(new Remote(ctx, auth).call()).isEmpty();
    }

    @Test(expected = IllegalStateException.class)
    public void loginFailOnUnknownException() {
        final AuthenticationHandler auth = Mockito.mock(AuthenticationHandler.class);
        Mockito.doThrow(IllegalStateException.class).when(auth).execute(Mockito.any(), Mockito.any());
        final Configuration ctx = Mockito.mock(Configuration.class);
        new Remote(ctx, auth).call();
    }

}
