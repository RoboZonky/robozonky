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

package com.github.triceo.robozonky.common.remote;

import java.util.Collections;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.ControlApi;
import com.github.triceo.robozonky.api.remote.EntityCollectionApi;
import com.github.triceo.robozonky.api.remote.LoanApi;
import com.github.triceo.robozonky.api.remote.PortfolioApi;
import com.github.triceo.robozonky.api.remote.WalletApi;
import com.github.triceo.robozonky.api.remote.entities.BlockedAmount;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Statistics;
import com.github.triceo.robozonky.api.remote.entities.Wallet;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AuthenticatedZonkyTest {

    private <T, S extends EntityCollectionApi<T>> PaginatedApi<T, S> mockApi() {
        final PaginatedApi<T, S> api = Mockito.mock(PaginatedApi.class);
        final PaginatedResult<T> apiReturn = new PaginatedResult<>(Collections.emptyList(), 0, 0);
        Mockito.when(api.execute(ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                .thenReturn(apiReturn);
        return api;
    }

    private <T> Api<T> mockApi(final T apiMock) {
        return new Api<>(apiMock, Mockito.mock(ResteasyClient.class));
    }

    @Test
    public void constructor() {
        final Api<ControlApi> ca = mockApi(Mockito.mock(ControlApi.class));
        final PaginatedApi<Loan, LoanApi> la = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        final PaginatedApi<Investment, PortfolioApi> pa = mockApi();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new AuthenticatedZonky(null, la, pa, wa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new AuthenticatedZonky(ca, null, pa, wa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new AuthenticatedZonky(ca, la, null, wa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new AuthenticatedZonky(ca, la, pa, null))
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    public void streams() {
        final Api<ControlApi> ca = mockApi(Mockito.mock(ControlApi.class));
        final PaginatedApi<Loan, LoanApi> la = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        final PaginatedApi<Investment, PortfolioApi> pa = mockApi();
        final AuthenticatedZonky z = new AuthenticatedZonky(ca, la, pa, wa);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(z.getAvailableLoans()).isEmpty();
            softly.assertThat(z.getBlockedAmounts()).isEmpty();
            softly.assertThat(z.getInvestments()).isEmpty();
        });
    }

    @Test
    public void investAndlogout() {
        final ControlApi control = Mockito.mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        Assertions.assertThat(ca.isClosed()).isFalse();
        final PaginatedApi<Loan, LoanApi> la = mockApi();
        final int loanId = 1;
        Mockito.when(la.execute((Function<LoanApi, Loan>) ArgumentMatchers.any())).thenReturn(new Loan(loanId, 200));
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        final PaginatedApi<Investment, PortfolioApi> pa = mockApi();
        try (final AuthenticatedZonky z = new AuthenticatedZonky(ca, la, pa, wa)) {
            final Loan l = z.getLoan(loanId);
            final Investment i = new Investment(l, 200);
            z.invest(i);
            z.logout();
        }
        Mockito.verify(control, Mockito.times(1)).invest(ArgumentMatchers.any());
        Mockito.verify(control, Mockito.times(1)).logout();
        Assertions.assertThat(ca.isClosed()).isTrue();
    }

    @Test
    public void wallet() {
        final ControlApi control = Mockito.mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        Assertions.assertThat(ca.isClosed()).isFalse();
        final PaginatedApi<Loan, LoanApi> la = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        Mockito.when(wa.execute((Function<WalletApi, Wallet>) ArgumentMatchers.any()))
                .thenReturn(Mockito.mock(Wallet.class));
        final PaginatedApi<Investment, PortfolioApi> pa = mockApi();
        try (final AuthenticatedZonky z = new AuthenticatedZonky(ca, la, pa, wa)) {
            final Wallet w = z.getWallet();
            Assertions.assertThat(w).isNotNull();
        }
    }

    @Test
    public void statistics() {
        final ControlApi control = Mockito.mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        Assertions.assertThat(ca.isClosed()).isFalse();
        final PaginatedApi<Loan, LoanApi> la = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        final PaginatedApi<Investment, PortfolioApi> pa = mockApi();
        Mockito.when(pa.execute((Function<PortfolioApi, Statistics>) ArgumentMatchers.any()))
                .thenReturn(Mockito.mock(Statistics.class));
        try (final AuthenticatedZonky z = new AuthenticatedZonky(ca, la, pa, wa)) {
            final Statistics s = z.getStatistics();
            Assertions.assertThat(s).isNotNull();
        }
    }

}
