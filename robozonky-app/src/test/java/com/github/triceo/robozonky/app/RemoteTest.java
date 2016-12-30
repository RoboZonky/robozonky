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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.OptionalInt;

import com.github.triceo.robozonky.Investor;
import com.github.triceo.robozonky.ZonkyProxy;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Wallet;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.configuration.Configuration;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class RemoteTest extends BaseMarketplaceTest {

    private static Configuration mockConfiguration(final boolean usesStrategy) {
        final Configuration ctx = Mockito.mock(Configuration.class);
        Mockito.when(ctx.getCaptchaDelay()).thenReturn(Duration.ofSeconds(120));
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

    private static Configuration mockConfiguration() {
        return RemoteTest.mockConfiguration(false);
    }

    @Test
    public void availableLoansRetrievalDuringSleep() {
        final Activity activity = Mockito.mock(Activity.class);
        Mockito.when(activity.shouldSleep()).thenReturn(true);
        Assertions.assertThat(Remote.getAvailableLoans(activity)).isEmpty();
    }

    @Test
    public void dryRunBalanceRetrieval() {
        // prepare context
        final BigDecimal dryRunBalance = BigDecimal.valueOf(12345);
        final Configuration ctx = Mockito.mock(Configuration.class);
        Mockito.when(ctx.isDryRun()).thenReturn(true);
        Mockito.when(ctx.getDryRunBalance()).thenReturn(OptionalInt.of(dryRunBalance.intValue()));
        // test operation
        Assertions.assertThat(Remote.getAvailableBalance(ctx, null)).isEqualTo(dryRunBalance);
    }

    @Test
    public void dryRunBalanceRetrievalUnspecified() {
        // prepare context
        final Configuration ctx = Mockito.mock(Configuration.class);
        Mockito.when(ctx.isDryRun()).thenReturn(true);
        Mockito.when(ctx.getDryRunBalance()).thenReturn(OptionalInt.empty());
        // prepare API
        final BigDecimal balance = BigDecimal.valueOf(12345);
        final Wallet wallet = Mockito.mock(Wallet.class);
        Mockito.when(wallet.getAvailableBalance()).thenReturn(balance);
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getWallet()).thenReturn(wallet);
        final ZonkyProxy proxy = new ZonkyProxy.Builder().build(api);
        // test operation
        Assertions.assertThat(Remote.getAvailableBalance(ctx, proxy)).isEqualTo(balance);
    }

    @Test
    public void standardBalanceRetrieval() {
        // prepare context
        final Configuration ctx = Mockito.mock(Configuration.class);
        Mockito.when(ctx.isDryRun()).thenReturn(false);
        // prepare API
        final BigDecimal balance = BigDecimal.valueOf(12345);
        final Wallet wallet = Mockito.mock(Wallet.class);
        Mockito.when(wallet.getAvailableBalance()).thenReturn(balance);
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getWallet()).thenReturn(wallet);
        final ZonkyProxy proxy = new ZonkyProxy.Builder().build(api);
        // test operation
        Assertions.assertThat(Remote.getAvailableBalance(ctx, proxy)).isEqualTo(balance);
    }

    @Test
    public void strategyDrivenInvestingFunction() {
        final Configuration ctx = RemoteTest.mockConfiguration(true);
        final Investor i = Mockito.mock(Investor.class);
        final Investment investment = Mockito.mock(Investment.class);
        Mockito.when(i.invest(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Collections.singletonList(investment));
        final Collection<Investment> result = Remote.getInvestingFunction(ctx, null).apply(i);
        Mockito.verify(i, Mockito.times(1)).invest(ArgumentMatchers.any(), ArgumentMatchers.any());
        Assertions.assertThat(result).containsExactly(investment);
    }

    @Test
    public void simpleInvestment() {
        final int loanId = 1;
        final int loanAmount = 1000;
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getId()).thenReturn(1);
        Mockito.when(mockLoan.getAmount()).thenReturn(loanAmount * 3.0);
        Mockito.when(mockLoan.getRemainingInvestment()).thenReturn(loanAmount * 2.0);
        Mockito.when(mockLoan.getDatePublished()).thenReturn(OffsetDateTime.now().minus(Duration.ofSeconds(30)));
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getLoan(ArgumentMatchers.eq(loanId))).thenReturn(mockLoan);
        Mockito.when(api.getWallet()).thenReturn(new Wallet(0, 0, BigDecimal.ZERO, BigDecimal.ZERO));
        final Configuration c = Mockito.mock(Configuration.class);
        Mockito.when(c.getZonkyProxyBuilder()).thenReturn(new ZonkyProxy.Builder().asDryRun());
        Mockito.when(c.getLoanId()).thenReturn(OptionalInt.of(loanId));
        Mockito.when(c.getLoanAmount()).thenReturn(OptionalInt.of(1000));
        Mockito.when(c.getCaptchaDelay()).thenReturn(Duration.ofMinutes(2));
        final Collection<Investment> result = Remote.invest(c, api, Collections.emptyList());
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void userDrivenInvestingFunction() {
        final Configuration ctx = RemoteTest.mockConfiguration(false);
        final Investor i = Mockito.mock(Investor.class);
        // check what happens when nothing is invested)
        Mockito.when(i.invest(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
        final Collection<Investment> result = Remote.getInvestingFunction(ctx, null).apply(i);
        Mockito.verify(i, Mockito.times(1))
                .invest(ctx.getLoanId().getAsInt(), ctx.getLoanAmount().getAsInt(), Duration.ofSeconds(120));
        Assertions.assertThat(result).isEmpty();
        // check what happens when something is invested
        final Investment investment = Mockito.mock(Investment.class);
        Mockito.when(i.invest(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.any()))
                .thenReturn(Optional.of(investment));
        final Collection<Investment> result2 = Remote.getInvestingFunction(ctx, null).apply(i);
        Assertions.assertThat(result2).containsExactly(investment);
    }

    @Test
    public void loginFailOnCredentials() {
        final AuthenticationHandler auth = Mockito.mock(AuthenticationHandler.class);
        Mockito.when(auth.execute(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Optional.empty());
        Assertions.assertThat(new Remote(RemoteTest.mockConfiguration(), auth).call()).isEmpty();
    }

    @Test(expected = IllegalStateException.class)
    public void loginFailOnUnknownException() {
        final AuthenticationHandler auth = Mockito.mock(AuthenticationHandler.class);
        Mockito.doThrow(IllegalStateException.class).when(auth).execute(ArgumentMatchers.any(), ArgumentMatchers.any());
        new Remote(RemoteTest.mockConfiguration(), auth).call();
    }

}
