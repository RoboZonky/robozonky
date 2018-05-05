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

import com.github.robozonky.api.remote.CollectionsApi;
import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.LoanApi;
import com.github.robozonky.api.remote.ParticipationApi;
import com.github.robozonky.api.remote.PortfolioApi;
import com.github.robozonky.api.remote.TransactionApi;
import com.github.robozonky.api.remote.WalletApi;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.RawDevelopment;
import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.entities.RawLoan;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class ZonkyTest {

    private <T, S> PaginatedApi<T, S> mockApi() {
        final PaginatedApi<T, S> api = mock(PaginatedApi.class);
        final PaginatedResult<T> apiReturn = new PaginatedResult<>(Collections.emptyList(), 0);
        when(api.execute(any(), any(), anyInt(), anyInt())).thenReturn(apiReturn);
        return api;
    }

    private <T> Api<T> mockApi(final T apiMock) {
        return new Api<>(apiMock);
    }

    private Zonky mockZonky(final Api<ControlApi> ca) {
        final PaginatedApi<RawLoan, LoanApi> la = mockApi();
        final PaginatedApi<Transaction, TransactionApi> ta = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        final PaginatedApi<RawInvestment, PortfolioApi> pa = mockApi();
        final PaginatedApi<Participation, ParticipationApi> sa = mockApi();
        final PaginatedApi<RawDevelopment, CollectionsApi> caa = mockApi();
        return new Zonky(ca, la, sa, pa, wa, ta, caa);
    }

    private Zonky mockZonky() {
        return mockZonky(mockApi(mock(ControlApi.class)));
    }

    @Test
    void constructor() {
        final Api<ControlApi> ca = mockApi(mock(ControlApi.class));
        final PaginatedApi<RawLoan, LoanApi> la = mockApi();
        final PaginatedApi<Transaction, TransactionApi> ta = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        final PaginatedApi<RawInvestment, PortfolioApi> pa = mockApi();
        final PaginatedApi<Participation, ParticipationApi> sa = mockApi();
        final PaginatedApi<RawDevelopment, CollectionsApi> caa = mockApi();
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new Zonky(null, la, sa, pa, wa, ta, caa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new Zonky(ca, null, sa, pa, wa, ta, caa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new Zonky(ca, la, null, pa, wa, ta, caa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new Zonky(ca, la, sa, null, wa, ta, caa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new Zonky(ca, la, sa, pa, null, ta, caa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new Zonky(ca, la, sa, pa, wa, null, caa))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new Zonky(ca, la, sa, pa, wa, ta, null))
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void loan() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final PaginatedApi<RawLoan, LoanApi> la = mockApi();
        final int loanId = 1;
        final RawLoan loan = mock(RawLoan.class);
        when(loan.getId()).thenReturn(loanId);
        when(loan.getAmount()).thenReturn(200.0);
        when(loan.getRemainingInvestment()).thenReturn(200.0);
        when(la.execute((Function<LoanApi, RawLoan>) any())).thenReturn(loan);
        final PaginatedApi<Transaction, TransactionApi> ta = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        final PaginatedApi<RawInvestment, PortfolioApi> pa = mockApi();
        final PaginatedApi<Participation, ParticipationApi> sa = mockApi();
        final PaginatedApi<RawDevelopment, CollectionsApi> caa = mockApi();
        final Zonky z = new Zonky(ca, la, sa, pa, wa, ta, caa);
        assertThat(z.getLoan(loanId).getId()).isEqualTo(loanId);
    }

    @Test
    void getters() {
        final Zonky z = mockZonky();
        assertSoftly(softly -> {
            softly.assertThat(z.getAvailableLoans(Select.unrestricted())).isEmpty();
            softly.assertThat(z.getBlockedAmounts()).isEmpty();
            softly.assertThat(z.getInvestments(Select.unrestricted())).isEmpty();
            softly.assertThat(z.getInvestment(1)).isEmpty();
            softly.assertThat(z.getAvailableParticipations(Select.unrestricted())).isEmpty();
            softly.assertThat(z.getTransactions(Select.unrestricted())).isEmpty();
            softly.assertThat(z.getDevelopments(Loan.custom().build())).isEmpty();
        });
    }

    @Test
    void investAndlogout() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final PaginatedApi<RawLoan, LoanApi> la = mockApi();
        final int loanId = 1;
        final RawLoan loan = mock(RawLoan.class);
        when(loan.getId()).thenReturn(loanId);
        when(loan.getAmount()).thenReturn(200.0);
        when(loan.getRemainingInvestment()).thenReturn(200.0);
        when(la.execute((Function<LoanApi, RawLoan>) any())).thenReturn(loan);
        final PaginatedApi<Transaction, TransactionApi> ta = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        final PaginatedApi<RawInvestment, PortfolioApi> pa = mockApi();
        final PaginatedApi<Participation, ParticipationApi> sa = mockApi();
        final PaginatedApi<RawDevelopment, CollectionsApi> caa = mockApi();
        final Zonky z = new Zonky(ca, la, sa, pa, wa, ta, caa);
        final Loan l = z.getLoan(loanId);
        final Investment i = Investment.fresh((MarketplaceLoan) l, 200);
        z.invest(i);
        z.logout();
        verify(control, times(1)).invest(any());
        verify(control, times(1)).logout();
    }

    @Test
    void wallet() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final PaginatedApi<RawLoan, LoanApi> la = mockApi();
        final PaginatedApi<Transaction, TransactionApi> ta = mockApi();
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        when(wa.execute((Function<WalletApi, Wallet>) any()))
                .thenReturn(mock(Wallet.class));
        final PaginatedApi<RawInvestment, PortfolioApi> pa = mockApi();
        final PaginatedApi<Participation, ParticipationApi> sa = mockApi();
        final PaginatedApi<RawDevelopment, CollectionsApi> caa = mockApi();
        final Zonky z = new Zonky(ca, la, sa, pa, wa, ta, caa);
        final Wallet w = z.getWallet();
        assertThat(w).isNotNull();
    }

    @Test
    void purchase() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonky(ca);
        final Participation p = mock(Participation.class);
        when(p.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
        when(p.getId()).thenReturn(1);
        z.purchase(p);
        verify(control).purchase(eq(p.getId()), any());
    }

    @Test
    void sell() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonky(ca);
        final Investment p = Investment.custom()
                .setRemainingPrincipal(BigDecimal.TEN)
                .setSmpFee(BigDecimal.ONE)
                .setId(1)
                .build();
        z.sell(p);
        verify(control).offer(any());
    }

    @Test
    void cancel() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonky(ca);
        final Investment i = mock(Investment.class);
        when(i.getId()).thenReturn(1);
        z.cancel(i);
        verify(control).cancel(eq(i.getId()));
    }
}
