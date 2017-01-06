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

import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.OptionalInt;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import com.github.triceo.robozonky.ApiProvider;
import com.github.triceo.robozonky.ZonkyProxy;
import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Wallet;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.authentication.SecretProvider;
import com.github.triceo.robozonky.app.configuration.Configuration;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AppTest extends AbstractStateLeveragingTest {

    private static Configuration mockDryRunConfiguration() {
        final Configuration ctx = Mockito.mock(Configuration.class);
        Mockito.when(ctx.getSleepPeriod()).thenReturn(Duration.ofMinutes(60));
        Mockito.when(ctx.getCaptchaDelay()).thenReturn(Duration.ofSeconds(120));
        Mockito.when(ctx.getLoanId()).thenReturn(OptionalInt.of(1));
        Mockito.when(ctx.getLoanAmount()).thenReturn(OptionalInt.of(1000));
        Mockito.when(ctx.getInvestmentStrategy()).thenReturn(Optional.empty());
        Mockito.when(ctx.isDryRun()).thenReturn(true);
        Mockito.when(ctx.getDryRunBalance()).thenReturn(OptionalInt.of(10000));
        return ctx;
    }

    private static Configuration mockRegularConfiguration() {
        final Configuration ctx = Mockito.mock(Configuration.class);
        Mockito.when(ctx.getSleepPeriod()).thenReturn(Duration.ofMinutes(60));
        Mockito.when(ctx.getCaptchaDelay()).thenReturn(Duration.ofSeconds(120));
        Mockito.when(ctx.getInvestmentStrategy()).thenReturn(Optional.of(Mockito.mock(InvestmentStrategy.class)));
        Mockito.when(ctx.isDryRun()).thenReturn(false);
        return ctx;
    }

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Test
    public void notWellFormedCli() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_WRONG_PARAMETERS.getCode());
        App.main();
    }

    @Test
    public void wrongStrategyOnCli() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_WRONG_PARAMETERS.getCode());
        App.main("-s", "some.random.file", "-u", "user", "-p", "password", "-t");
    }

    @Test
    public void wrongKeyStore() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_WRONG_PARAMETERS.getCode());
        App.main("-l", "1", "-a", "1000", "-g", "some.random.file", "-p", "password");
    }

    @Test
    public void handleUnexpectedError() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_UNEXPECTED.getCode());
        App.handleUnexpectedException(null);
    }

    @Test
    public void handleMaintenanceError() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_DOWN.getCode());
        App.handleZonkyMaintenanceError(null, false);
    }

    @Test
    public void handleMaintenanceErrorFaultTolerant() {
        exit.expectSystemExitWithStatus(ReturnCode.OK.getCode());
        App.handleZonkyMaintenanceError(null, true);
    }

    @Test
    public void handleProcessingExceptionWithoutCase() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_UNEXPECTED.getCode());
        App.handleException(new ProcessingException("No cause."), false);
    }

    @Test
    public void handleProcessingExceptionOkCausedBySocket() {
        exit.expectSystemExitWithStatus(ReturnCode.OK.getCode());
        App.handleException(new ProcessingException(new SocketException()), true);
    }

    @Test
    public void handleProcessingExceptionOkCausedByHost() {
        exit.expectSystemExitWithStatus(ReturnCode.OK.getCode());
        App.handleException(new ProcessingException(new UnknownHostException()), true);
    }

    @Test
    public void handleProcessingExceptionDownCausedBySocket() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_DOWN.getCode());
        App.handleException(new ProcessingException(new SocketException()), false);
    }

    @Test
    public void handleProcessingExceptionDownCausedByHost() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_DOWN.getCode());
        App.handleException(new ProcessingException(new UnknownHostException()), false);
    }

    @Test
    public void handleException() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_REMOTE.getCode());
        App.handleException(new WebApplicationException());
    }

    @Test
    public void singleInvestmentExecutionFailingLogin() {
        // a lot of mocking to exercise the basic path all the way through to the core
        final Configuration ctx = AppTest.mockDryRunConfiguration();
        Mockito.when(ctx.getZonkyProxyBuilder()).thenReturn(new ZonkyProxy.Builder().asDryRun());
        final SecretProvider secret = Mockito.mock(SecretProvider.class);
        Mockito.when(secret.getPassword()).thenReturn("".toCharArray());
        final AuthenticationHandler auth = Mockito.mock(AuthenticationHandler.class);
        final ApiProvider api = Mockito.mock(ApiProvider.class);
        Mockito.when(api.oauth()).thenReturn(Mockito.mock(ZonkyOAuthApi.class));
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now());
        final ZonkyApi zonky = Mockito.mock(ZonkyApi.class);
        Mockito.when(zonky.getLoan(ArgumentMatchers.anyInt())).thenReturn(loan);
        Mockito.when(zonky.getWallet()).thenReturn(Mockito.mock(Wallet.class));
        Mockito.when(api.authenticated(ArgumentMatchers.any())).thenReturn(zonky);
        // and now test
        final ReturnCode rc = App.execute(ctx, auth);
        Assertions.assertThat(rc).isEqualTo(ReturnCode.ERROR_SETUP);
    }

    @Test
    public void strategyBasedExecutionInvestingNothing() {
        // a lot of mocking to exercise the basic path all the way through to the core
        final Configuration ctx = AppTest.mockRegularConfiguration();
        Mockito.when(ctx.getZonkyProxyBuilder()).thenReturn(new ZonkyProxy.Builder().asDryRun());
        final SecretProvider secret = Mockito.mock(SecretProvider.class);
        Mockito.when(secret.getPassword()).thenReturn("".toCharArray());
        final AuthenticationHandler auth = Mockito.mock(AuthenticationHandler.class);
        Mockito.when(auth.execute(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(Optional.of(Collections.emptyList()));
        final ApiProvider api = Mockito.mock(ApiProvider.class);
        Mockito.when(api.oauth()).thenReturn(Mockito.mock(ZonkyOAuthApi.class));
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now());
        final ZonkyApi zonky = Mockito.mock(ZonkyApi.class);
        Mockito.when(zonky.getLoan(ArgumentMatchers.anyInt())).thenReturn(loan);
        Mockito.when(zonky.getWallet()).thenReturn(Mockito.mock(Wallet.class));
        Mockito.when(api.authenticated(ArgumentMatchers.any())).thenReturn(zonky);
        // and now test
        final ReturnCode rc = App.execute(ctx, auth);
        Assertions.assertThat(rc).isEqualTo(ReturnCode.OK);
    }

}
