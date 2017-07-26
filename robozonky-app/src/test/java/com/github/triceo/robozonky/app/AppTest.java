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

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.investing.DirectInvestmentMode;
import com.github.triceo.robozonky.app.investing.InvestmentMode;
import com.github.triceo.robozonky.app.investing.Investor;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.github.triceo.robozonky.common.remote.OAuth;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AppTest extends AbstractEventsAndStateLeveragingTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Test
    public void notWellFormedCli() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_WRONG_PARAMETERS.getCode());
        App.main();
    }

    @Test
    public void wrongKeyStore() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_WRONG_PARAMETERS.getCode());
        App.main("-g", "some.random.file", "-p", "password", "single", "-l", "1", "-a", "1000");
    }

    @Test
    public void singleInvestmentExecutionFailingLogin() {
        // a lot of mocking to exercise the basic path all the way through to the core
        final SecretProvider secret = Mockito.mock(SecretProvider.class);
        Mockito.when(secret.getPassword()).thenReturn("".toCharArray());
        final Authenticated auth = Mockito.mock(Authenticated.class);
        Mockito.doThrow(IllegalStateException.class).when(auth).execute(ArgumentMatchers.any());
        final ApiProvider api = Mockito.mock(ApiProvider.class);
        Mockito.when(api.oauth()).thenReturn(Mockito.mock(OAuth.class));
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now());
        // and now test
        final ReturnCode rc = App.execute(new DirectInvestmentMode(auth, new Investor.Builder().asDryRun(),
                                                                   false, 1, 1000));
        Assertions.assertThat(rc).isEqualTo(ReturnCode.ERROR_SETUP);
    }

    @Test
    public void modeProcessed() throws Exception {
        final AtomicBoolean faultTolerant = new AtomicBoolean(false);
        final InvestmentMode mode = Mockito.mock(InvestmentMode.class);
        Mockito.when(mode.get()).thenReturn(Optional.empty());
        Mockito.when(mode.isFaultTolerant()).thenReturn(!faultTolerant.get());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(App.execute(mode, faultTolerant)).isEqualTo(ReturnCode.ERROR_SETUP);
            softly.assertThat(faultTolerant.get()).isTrue();
            softly.assertThat(App.SHUTDOWN_HOOKS.getRegisteredCount()).isGreaterThanOrEqualTo(3);
        });
        Mockito.verify(mode).close();
    }
}
