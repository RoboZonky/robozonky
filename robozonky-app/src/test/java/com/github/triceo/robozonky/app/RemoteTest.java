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

package com.github.triceo.robozonky.app;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import com.github.triceo.robozonky.ApiProvider;
import com.github.triceo.robozonky.ZonkyProxy;
import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.triceo.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.triceo.robozonky.api.notifications.LoanArrivedEvent;
import com.github.triceo.robozonky.api.notifications.MarketplaceCheckCompletedEvent;
import com.github.triceo.robozonky.api.notifications.MarketplaceCheckStartedEvent;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.api.remote.ZotifyApi;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Wallet;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.authentication.SecretProvider;
import com.github.triceo.robozonky.app.configuration.Configuration;
import com.github.triceo.robozonky.notifications.Events;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class RemoteTest extends AbstractStateLeveragingTest {

    private static Configuration mockConfiguration(final boolean usesStrategy) {
        final Configuration ctx = Mockito.mock(Configuration.class);
        Mockito.when(ctx.getSleepPeriod()).thenReturn(Duration.ofMinutes(60));
        Mockito.when(ctx.getCaptchaDelay()).thenReturn(Duration.ofSeconds(120));
        Mockito.when(ctx.getLoanId()).thenReturn(OptionalInt.of(1));
        Mockito.when(ctx.getLoanAmount()).thenReturn(OptionalInt.of(1000));
        if (usesStrategy) {
            final InvestmentStrategy strategyMock = Mockito.mock(InvestmentStrategy.class);
            final Refreshable<InvestmentStrategy> refreshable = Mockito.mock(Refreshable.class);
            Mockito.when(refreshable.getLatest()).thenReturn(Optional.of(strategyMock));
            Mockito.when(ctx.getInvestmentStrategy()).thenReturn(Optional.of(refreshable));
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

    @Test
    public void singleInvestmentExecutionInvestingNothing() {
        // a lot of mocking to exercise the basic path all the way through to the core
        final Configuration ctx = RemoteTest.mockConfiguration(false);
        Mockito.when(ctx.getZonkyProxyBuilder()).thenReturn(new ZonkyProxy.Builder().asDryRun());
        final SecretProvider secret = Mockito.mock(SecretProvider.class);
        Mockito.when(secret.getPassword()).thenReturn("".toCharArray());
        final AuthenticationHandler auth = AuthenticationHandler.passwordBased(secret);
        final ApiProvider api = Mockito.mock(ApiProvider.class);
        Mockito.when(api.oauth()).thenReturn(Mockito.mock(ZonkyOAuthApi.class));
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now());
        final ZonkyApi zonky = Mockito.mock(ZonkyApi.class);
        Mockito.when(zonky.getLoan(ArgumentMatchers.anyInt())).thenReturn(loan);
        Mockito.when(zonky.getWallet()).thenReturn(Mockito.mock(Wallet.class));
        Mockito.when(api.authenticated(ArgumentMatchers.any())).thenReturn(zonky);
        // and now test
        final Remote r = new Remote(ctx, auth);
        final Optional<Collection<Investment>> result = r.executeSingleInvestment(api);
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get()).isEmpty();
    }

    @Test
    public void strategyExecutionUnderBalance() {
        // a lot of mocking to exercise the basic path all the way through to the core
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getDatePublished()).thenReturn(OffsetDateTime.now());
        Mockito.when(l.getRemainingInvestment()).thenReturn(1000.0);
        final Configuration ctx = RemoteTest.mockConfiguration(true);
        Mockito.when(ctx.getZonkyProxyBuilder()).thenReturn(new ZonkyProxy.Builder().asDryRun());
        final SecretProvider secret = Mockito.mock(SecretProvider.class);
        Mockito.when(secret.getPassword()).thenReturn("".toCharArray());
        final AuthenticationHandler auth = AuthenticationHandler.passwordBased(secret);
        final ZotifyApi cache = Mockito.mock(ZotifyApi.class);
        Mockito.when(cache.getLoans()).thenReturn(Collections.singletonList(l));
        final ApiProvider api = Mockito.mock(ApiProvider.class);
        Mockito.when(api.cache()).thenReturn(cache);
        Mockito.when(api.oauth()).thenReturn(Mockito.mock(ZonkyOAuthApi.class));
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now());
        final ZonkyApi zonky = Mockito.mock(ZonkyApi.class);
        Mockito.when(zonky.getLoan(ArgumentMatchers.anyInt())).thenReturn(loan);
        final Wallet wallet = Mockito.mock(Wallet.class);
        Mockito.when(wallet.getAvailableBalance()).thenReturn(BigDecimal.TEN);
        Mockito.when(zonky.getWallet()).thenReturn(wallet);
        Mockito.when(api.authenticated(ArgumentMatchers.any())).thenReturn(zonky);
        // and now test
        final Optional<Collection<Investment>> result = new Remote(ctx, auth).executeStrategy(api);
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get()).isEmpty();
        // check events
        final List<Event> fired = Events.getFired();
        final List<Event> lastFired = fired.subList(fired.size() - 5, fired.size());
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(lastFired.get(0)).isInstanceOf(MarketplaceCheckStartedEvent.class);
        softly.assertThat(lastFired.get(1)).isInstanceOf(LoanArrivedEvent.class);
        softly.assertThat(lastFired.get(2)).isInstanceOf(MarketplaceCheckCompletedEvent.class);
        softly.assertThat(lastFired.get(3)).isInstanceOf(ExecutionStartedEvent.class);
        softly.assertThat(lastFired.get(4)).isInstanceOf(ExecutionCompletedEvent.class);
        softly.assertAll();
    }

}
