/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.common.remote;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.function.Function;

import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.EntityCollectionApi;
import com.github.robozonky.api.remote.LoanApi;
import com.github.robozonky.api.remote.ParticipationApi;
import com.github.robozonky.api.remote.PortfolioApi;
import com.github.robozonky.api.remote.WalletApi;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.Wallet;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class ZonkyTest {

    private <T, S extends EntityCollectionApi<T>> PaginatedApi<T, S> mockApi() {
        final PaginatedApi<T, S> api = Mockito.mock(PaginatedApi.class);
        final PaginatedResult<T> apiReturn = new PaginatedResult<>(Collections.emptyList(), 0, 0);
        Mockito.when(api.execute(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                                 ArgumentMatchers.anyInt()))
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
        final PaginatedApi<Participation, ParticipationApi> sa = mockApi();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new Zonky(null, la, sa, pa, wa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new Zonky(ca, null, sa, pa, wa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new Zonky(ca, la, null, pa, wa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new Zonky(ca, la, sa, null, wa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new Zonky(ca, la, sa, pa, null))
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    public void streams() {
        final Api<ControlApi> ca = mockApi(Mockito.mock(ControlApi.class));
        final PaginatedApi<Loan, LoanApi> la = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        final PaginatedApi<Investment, PortfolioApi> pa = mockApi();
        final PaginatedApi<Participation, ParticipationApi> sa = mockApi();
        final Zonky z = new Zonky(ca, la, sa, pa, wa);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(z.getAvailableLoans()).isEmpty();
            softly.assertThat(z.getAvailableLoans(Sort.unspecified())).isEmpty();
            softly.assertThat(z.getBlockedAmounts()).isEmpty();
            softly.assertThat(z.getInvestments()).isEmpty();
            softly.assertThat(z.getInvestments(Sort.unspecified())).isEmpty();
            softly.assertThat(z.getAvailableParticipations()).isEmpty();
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
        final PaginatedApi<Participation, ParticipationApi> sa = mockApi();
        try (final Zonky z = new Zonky(ca, la, sa, pa, wa)) {
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
        final PaginatedApi<Participation, ParticipationApi> sa = mockApi();
        try (final Zonky z = new Zonky(ca, la, sa, pa, wa)) {
            final Wallet w = z.getWallet();
            Assertions.assertThat(w).isNotNull();
        }
    }

    @Test
    public void purchase() {
        final ControlApi control = Mockito.mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        Assertions.assertThat(ca.isClosed()).isFalse();
        final PaginatedApi<Loan, LoanApi> la = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        final PaginatedApi<Investment, PortfolioApi> pa = mockApi();
        final PaginatedApi<Participation, ParticipationApi> sa = mockApi();
        try (final Zonky z = new Zonky(ca, la, sa, pa, wa)) {
            final Participation p = Mockito.mock(Participation.class);
            Mockito.when(p.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
            Mockito.when(p.getId()).thenReturn(1);
            z.purchase(p);
            Mockito.verify(control).purchase(ArgumentMatchers.eq(p.getId()), ArgumentMatchers.any());
        }
    }

    @Test
    public void sell() {
        final ControlApi control = Mockito.mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        Assertions.assertThat(ca.isClosed()).isFalse();
        final PaginatedApi<Loan, LoanApi> la = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        final PaginatedApi<Investment, PortfolioApi> pa = mockApi();
        final PaginatedApi<Participation, ParticipationApi> sa = mockApi();
        try (final Zonky z = new Zonky(ca, la, sa, pa, wa)) {
            final Investment p = Mockito.mock(Investment.class);
            Mockito.when(p.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
            Mockito.when(p.getSmpFee()).thenReturn(BigDecimal.ONE);
            Mockito.when(p.getId()).thenReturn(1);
            z.sell(p);
            Mockito.verify(control).offer(ArgumentMatchers.any());
        }
    }

    @Test
    public void cancel() {
        final ControlApi control = Mockito.mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        Assertions.assertThat(ca.isClosed()).isFalse();
        final PaginatedApi<Loan, LoanApi> la = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        final PaginatedApi<Investment, PortfolioApi> pa = mockApi();
        final PaginatedApi<Participation, ParticipationApi> sa = mockApi();
        try (final Zonky z = new Zonky(ca, la, sa, pa, wa)) {
            final Investment i = Mockito.mock(Investment.class);
            Mockito.when(i.getId()).thenReturn(1);
            z.cancel(i);
            Mockito.verify(control).cancel(ArgumentMatchers.eq(i.getId()));
        }
    }

}
