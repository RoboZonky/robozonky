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

import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Wallet;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.common.remote.ApiProvider;
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
        final ZonkyApi z = Mockito.mock(ZonkyApi.class);
        Mockito.when(z.getWallet()).thenReturn(w);
        Mockito.when(z.getLoan(ArgumentMatchers.anyInt())).thenReturn(l);
        final ApiProvider p = Mockito.mock(ApiProvider.class);
        Mockito.when(p.authenticated(ArgumentMatchers.any()))
                .thenReturn(new ApiProvider.ApiWrapper<>(z));
        Mockito.when(p.oauth()).thenReturn(Mockito.mock(ApiProvider.ApiWrapper.class));
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
        final ZonkyApi z = Mockito.mock(ZonkyApi.class);
        Mockito.when(z.getWallet()).thenReturn(w);
        Mockito.doThrow(IllegalStateException.class).when(z).getLoan(ArgumentMatchers.anyInt());
        final ApiProvider p = Mockito.mock(ApiProvider.class);
        Mockito.when(p.authenticated(ArgumentMatchers.any()))
                .thenReturn(new ApiProvider.ApiWrapper<>(z));
        Mockito.when(p.oauth()).thenReturn(Mockito.mock(ApiProvider.ApiWrapper.class));
        try (final DirectInvestmentMode exec = new DirectInvestmentMode(
                AuthenticationHandler.passwordBased(SecretProvider.fallback("username", new char[0])),
                new ZonkyProxy.Builder().asDryRun(), true, l.getId(), (int)l.getAmount())) {
            Assertions.assertThat(exec.execute(p)).isEmpty();
        } catch (final Exception ex) {
            Assertions.fail("Unexpected exception.", ex);
        }
    }

}
