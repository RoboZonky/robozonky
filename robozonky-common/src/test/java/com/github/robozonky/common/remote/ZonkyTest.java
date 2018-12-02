/*
 * Copyright 2018 The RoboZonky Project
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;

import com.github.robozonky.api.remote.CollectionsApi;
import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.EntityCollectionApi;
import com.github.robozonky.api.remote.LoanApi;
import com.github.robozonky.api.remote.ParticipationApi;
import com.github.robozonky.api.remote.PortfolioApi;
import com.github.robozonky.api.remote.TransactionApi;
import com.github.robozonky.api.remote.WalletApi;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.entities.RawLoan;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZonkyTest {

    private static <T, S> PaginatedApi<T, S> mockApi() {
        return mockApi(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    private static <T, S> PaginatedApi<T, S> mockApi(final List<T> toReturn) {
        final PaginatedApi<T, S> api = mock(PaginatedApi.class);
        final PaginatedResult<T> apiReturn = new PaginatedResult<T>(toReturn, toReturn.size());
        when(api.execute(any(), any(), anyInt(), anyInt())).thenReturn(apiReturn);
        return api;
    }

    private static <T> Api<T> mockApi(final T apiMock) {
        return new Api<>(apiMock);
    }

    private static Zonky mockZonkyControl(final Api<ControlApi> ca) {
        final ApiProvider apiProvider = mockApiProvider();
        doReturn(ca).when(apiProvider).obtainNormal(eq(ControlApi.class), any());
        return new Zonky(apiProvider, () -> mock(ZonkyApiToken.class));
    }

    private static Zonky mockZonkyExports(final Api<ExportApi> ea) {
        final ApiProvider apiProvider = mockApiProvider();
        when(apiProvider.exports(any())).thenReturn(ea);
        return new Zonky(apiProvider, () -> mock(ZonkyApiToken.class));
    }

    private static Zonky mockZonkyWallet(final PaginatedApi<BlockedAmount, WalletApi> wa) {
        final ApiProvider apiProvider = mockApiProvider();
        when(apiProvider.obtainPaginated(eq(WalletApi.class), any(), any())).thenReturn(wa);
        return new Zonky(apiProvider, () -> mock(ZonkyApiToken.class));
    }

    private static <S, T extends EntityCollectionApi<S>> void mockPaginated(final ApiProvider apiProvider,
                                                                            final Class<T> blueprint,
                                                                            final PaginatedApi<S, T> api) {
        when(apiProvider.obtainPaginated(eq(blueprint), any(), any())).thenReturn(api);
    }

    private static <S, T extends EntityCollectionApi<S>> void mockPaginated(final ApiProvider apiProvider,
                                                                            final Class<T> blueprint) {
        mockPaginated(apiProvider, blueprint, mockApi());
    }

    private static ApiProvider mockApiProvider() {
        final ApiProvider apiProvider = spy(new ApiProvider());
        final Api<ControlApi> ca = ApiProvider.obtainNormal(mock(ControlApi.class));
        doReturn(ca).when(apiProvider).obtainNormal(eq(ControlApi.class), any());
        final Api<ExportApi> ea = mockApi(mock(ExportApi.class));
        when(apiProvider.exports(any())).thenReturn(ea);
        mockPaginated(apiProvider, WalletApi.class);
        mockPaginated(apiProvider, LoanApi.class);
        mockPaginated(apiProvider, TransactionApi.class);
        mockPaginated(apiProvider, PortfolioApi.class);
        mockPaginated(apiProvider, ParticipationApi.class);
        mockPaginated(apiProvider, CollectionsApi.class);
        return apiProvider;
    }

    private static Zonky mockZonky(final PaginatedApi<RawInvestment, PortfolioApi> pa,
                                   final PaginatedApi<Transaction, TransactionApi> ta) {
        final ApiProvider apiProvider = mockApiProvider();
        mockPaginated(apiProvider, PortfolioApi.class, pa);
        mockPaginated(apiProvider, TransactionApi.class, ta);
        return new Zonky(apiProvider, () -> mock(ZonkyApiToken.class));
    }

    private static Zonky mockZonky(final Api<ControlApi> ca, final PaginatedApi<RawLoan, LoanApi> la) {
        final ApiProvider apiProvider = mockApiProvider();
        doReturn(ca).when(apiProvider).obtainNormal(eq(ControlApi.class), any());
        mockPaginated(apiProvider, LoanApi.class, la);
        return new Zonky(apiProvider, () -> mock(ZonkyApiToken.class));
    }

    private static Zonky mockZonky() {
        final ApiProvider apiProvider = mockApiProvider();
        return new Zonky(apiProvider, () -> mock(ZonkyApiToken.class));
    }

    @Test
    void loan() {
        final PaginatedApi<RawLoan, LoanApi> la = mockApi();
        final int loanId = 1;
        final RawLoan loan = mock(RawLoan.class);
        when(loan.getId()).thenReturn(loanId);
        when(loan.getAmount()).thenReturn(200.0);
        when(loan.getRemainingInvestment()).thenReturn(200.0);
        when(la.execute(any())).thenReturn(loan);
        final ApiProvider p = spy(new ApiProvider());
        when(p.marketplace(any())).thenReturn(la);
        final Zonky z = new Zonky(p, () -> mock(ZonkyApiToken.class));
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
            softly.assertThat(z.getDevelopments(1)).isEmpty();
        });
    }

    @Test
    void exports() {
        final ExportApi api = mock(ExportApi.class);
        when(api.downloadInvestmentsExport()).thenReturn(mock(Response.class));
        when(api.downloadWalletExport()).thenReturn(mock(Response.class));
        final Api<ExportApi> ea = mockApi(api);
        final Zonky z = mockZonkyExports(ea);
        assertSoftly(softly -> {
            softly.assertThat(z.downloadInvestmentsExport()).isNotNull();
            softly.assertThat(z.downloadWalletExport()).isNotNull();
        });
        z.requestInvestmentsExport();
        verify(api).requestInvestmentsExport();
        z.requestWalletExport();
        verify(api).requestWalletExport();
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
        when(la.execute(any())).thenReturn(loan);
        final Zonky z = mockZonky(ca, la);
        final Loan l = z.getLoan(loanId);
        final Investment i = Investment.fresh(l, 200);
        z.invest(i);
        z.logout();
        verify(control, times(1)).invest(any());
        verify(control, times(1)).logout();
    }

    @Test
    void wallet() {
        final PaginatedApi<BlockedAmount, WalletApi> wa = mockApi();
        when(wa.execute(any())).thenReturn(mock(Wallet.class));
        final Zonky z = mockZonkyWallet(wa);
        final Wallet w = z.getWallet();
        assertThat(w).isNotNull();
    }

    @Test
    void purchase() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonkyControl(ca);
        final Participation p = mock(Participation.class);
        when(p.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
        when(p.getId()).thenReturn(1L);
        z.purchase(p);
        verify(control).purchase(eq(p.getId()), any());
    }

    @Test
    void sell() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonkyControl(ca);
        final Investment p = Investment.custom()
                .setRemainingPrincipal(BigDecimal.TEN)
                .setSmpFee(BigDecimal.ONE)
                .setId(1)
                .build();
        z.sell(p);
        verify(control).offer(any());
    }

    @Test
    void correctlyGuessesInvestmentDateFromTransactionHistory() {
        final Loan loan = Loan.custom()
                .setId(1)
                .build();
        final Investment i = Investment.fresh(loan, 200)
                .build();
        final RawInvestment r = spy(new RawInvestment(i));
        when(r.getInvestmentDate()).thenReturn(null); // enforce so that the date-guessing code has a chance to trigger
        final PaginatedApi<RawInvestment, PortfolioApi> pa = mockApi(Collections.singletonList(r));
        final Transaction irrelevant1 =
                new Transaction(i, BigDecimal.ZERO, TransactionCategory.SMP_BUY, TransactionOrientation.OUT);
        final Transaction irrelevant2 =
                new Transaction(i, BigDecimal.ZERO, TransactionCategory.SMP_SELL, TransactionOrientation.IN);
        final Transaction relevant =
                new Transaction(i, BigDecimal.ZERO, TransactionCategory.PAYMENT, TransactionOrientation.IN);
        final PaginatedApi<Transaction, TransactionApi> ta = mockApi(Arrays.asList(irrelevant1, relevant, irrelevant2));
        final Zonky z = mockZonky(pa, ta);
        final Optional<Investment> result = z.getInvestmentByLoanId(loan.getId());
        assertThat(result).isPresent();
        final Investment actual = result.get();
        final LocalDate investmentDate = actual.getInvestmentDate().toLocalDate();
        assertThat(investmentDate).isEqualTo(relevant.getTransactionDate().minusMonths(1));
    }

    @Test
    void cancel() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonkyControl(ca);
        final Investment i = mock(Investment.class);
        when(i.getId()).thenReturn(1L);
        z.cancel(i);
        verify(control).cancel(eq(i.getId()));
    }
}
