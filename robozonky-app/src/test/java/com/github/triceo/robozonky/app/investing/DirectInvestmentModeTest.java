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

package com.github.triceo.robozonky.app.investing;

import java.math.BigDecimal;
import java.util.Collections;

import com.github.triceo.robozonky.api.remote.ControlApi;
import com.github.triceo.robozonky.api.remote.LoanApi;
import com.github.triceo.robozonky.api.remote.PortfolioApi;
import com.github.triceo.robozonky.api.remote.WalletApi;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Statistics;
import com.github.triceo.robozonky.api.remote.entities.Wallet;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.common.remote.Apis;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class DirectInvestmentModeTest extends AbstractInvestingTest{

    @Test
    public void standard() {
        final Loan l = new Loan(1, 1000);
        final Wallet w = new Wallet(1, 2, BigDecimal.TEN, BigDecimal.ZERO);
        final ControlApi z = Mockito.mock(ControlApi.class);
        final Apis p = Mockito.spy(Apis.class);
        Mockito.doReturn(new Apis.Wrapper<>(z)).when(p).control(ArgumentMatchers.any());
        final WalletApi wa = Mockito.mock(WalletApi.class);
        Mockito.when(wa.wallet()).thenReturn(w);
        Mockito.doReturn(new Apis.Wrapper<>(wa)).when(p).wallet(ArgumentMatchers.any());
        final LoanApi la = Mockito.mock(LoanApi.class);
        Mockito.when(la.item(ArgumentMatchers.anyInt())).thenReturn(l);
        Mockito.when(la.items()).thenReturn(Collections.singletonList(l));
        Mockito.doReturn(new Apis.Wrapper<>(la)).when(p).loans(ArgumentMatchers.any());
        final PortfolioApi pa = Mockito.mock(PortfolioApi.class);
        Mockito.when(pa.statistics()).thenReturn(Mockito.mock(Statistics.class));
        Mockito.when(pa.items()).thenReturn(Collections.emptyList());
        Mockito.doReturn(new Apis.Wrapper<>(pa)).when(p).portfolio(ArgumentMatchers.any());
        Mockito.doReturn(Mockito.mock(Apis.Wrapper.class)).when(p).oauth();
        try (final DirectInvestmentMode exec = new DirectInvestmentMode(
                AuthenticationHandler.passwordBased(SecretProvider.fallback("username", new char[0])),
                new ZonkyProxy.Builder().asDryRun(), true, l.getId(), (int)l.getAmount())) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(exec.execute(p)).isPresent();
                softly.assertThat(exec.isFaultTolerant()).isTrue();
                softly.assertThat(exec.isDryRun()).isTrue();
            });
        } catch (final Exception ex) {
            Assertions.fail("Unexpected exception.", ex);
        }
    }

    @Test
    public void failingDuringInvest() {
        final Loan l = new Loan(1, 1000);
        final Wallet w = new Wallet(1, 2, BigDecimal.TEN, BigDecimal.ZERO);
        final ControlApi z = Mockito.mock(ControlApi.class);
        final Apis p = Mockito.mock(Apis.class);
        final WalletApi wa = Mockito.mock(WalletApi.class);
        Mockito.when(wa.wallet()).thenReturn(w);
        Mockito.when(p.wallet(ArgumentMatchers.any())).thenReturn(new Apis.Wrapper<>(wa));
        final LoanApi la = Mockito.mock(LoanApi.class);
        Mockito.doThrow(IllegalStateException.class).when(la).item(ArgumentMatchers.anyInt());
        Mockito.when(p.loans(ArgumentMatchers.any())).thenReturn(new Apis.Wrapper<>(la));
        Mockito.when(p.control(ArgumentMatchers.any())).thenReturn(new Apis.Wrapper<>(z));
        Mockito.when(p.oauth()).thenReturn(Mockito.mock(Apis.Wrapper.class));
        try (final DirectInvestmentMode exec = new DirectInvestmentMode(
                AuthenticationHandler.passwordBased(SecretProvider.fallback("username", new char[0])),
                new ZonkyProxy.Builder().asDryRun(), true, l.getId(), (int)l.getAmount())) {
            Assertions.assertThat(exec.execute(p)).isEmpty();
        } catch (final Exception ex) {
            Assertions.fail("Unexpected exception.", ex);
        }
    }

}

