/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.remote;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.LastPublishedItem;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.MyReservation;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.ParticipationDetail;
import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.remote.endpoints.ControlApi;
import com.github.robozonky.internal.remote.endpoints.EntityCollectionApi;
import com.github.robozonky.internal.remote.endpoints.LoanApi;
import com.github.robozonky.internal.remote.endpoints.ParticipationApi;
import com.github.robozonky.internal.remote.endpoints.PortfolioApi;
import com.github.robozonky.internal.remote.entities.AmountsImpl;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.remote.entities.InvestmentLoanDataImpl;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.remote.entities.MyReservationImpl;
import com.github.robozonky.internal.remote.entities.ParticipationImpl;
import com.github.robozonky.internal.remote.entities.ReservationImpl;
import com.github.robozonky.internal.remote.entities.ReservationPreferencesImpl;
import com.github.robozonky.internal.remote.entities.ResolutionRequest;
import com.github.robozonky.internal.remote.entities.RestrictionsImpl;
import com.github.robozonky.internal.remote.entities.SellInfoImpl;
import com.github.robozonky.internal.remote.entities.ZonkyApiTokenImpl;

class ZonkyTest {

    private static <T, S> PaginatedApi<T, S> mockApi() {
        return mockApi(Collections.emptyList());
    }

    private static InvestmentImpl mockInvestment(final Loan loan, final int amount) {
        return new InvestmentImpl(new InvestmentLoanDataImpl(loan), Money.from(amount));
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
        doReturn(ca).when(apiProvider)
            .obtainNormal(eq(ControlApi.class), any());
        return new Zonky(apiProvider, () -> mock(ZonkyApiTokenImpl.class));
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
        final ControlApi camock = mock(ControlApi.class);
        when(camock.restrictions()).thenReturn(new RestrictionsImpl());
        final Api<ControlApi> ca = ApiProvider.actuallyObtainNormal(camock, null);
        doReturn(ca).when(apiProvider)
            .obtainNormal(eq(ControlApi.class), any());
        mockPaginated(apiProvider, LoanApi.class);
        mockPaginated(apiProvider, PortfolioApi.class);
        mockPaginated(apiProvider, ParticipationApi.class);
        return apiProvider;
    }

    private static Zonky mockZonky(final Api<ControlApi> ca, final PaginatedApi<LoanImpl, LoanApi> la) {
        final ApiProvider apiProvider = mockApiProvider();
        doReturn(ca).when(apiProvider)
            .obtainNormal(eq(ControlApi.class), any());
        mockPaginated(apiProvider, LoanApi.class, la);
        return new Zonky(apiProvider, () -> mock(ZonkyApiTokenImpl.class));
    }

    private static Zonky mockZonky() {
        final ApiProvider apiProvider = mockApiProvider();
        return new Zonky(apiProvider, () -> mock(ZonkyApiTokenImpl.class));
    }

    private static Zonky mockZonky(final ApiProvider apiProvider) {
        return new Zonky(apiProvider, () -> mock(ZonkyApiTokenImpl.class));
    }

    @Test
    void loan() {
        final PaginatedApi<LoanImpl, LoanApi> la = mockApi();
        final int loanId = 1;
        final Loan loan = mock(LoanImpl.class);
        when(loan.getId()).thenReturn(loanId);
        when(loan.getAmount()).thenReturn(Money.from(200.0));
        when(loan.getRemainingInvestment()).thenReturn(Money.from(200.0));
        when(la.execute(any())).thenReturn(loan);
        when(la.execute(any(), anyBoolean())).thenReturn(mock(LastPublishedItem.class));
        final ApiProvider p = spy(new ApiProvider());
        when(p.marketplace(any())).thenReturn(la);
        final Zonky z = new Zonky(p, () -> mock(ZonkyApiTokenImpl.class));
        assertThat(z.getLoan(loanId)
            .getId()).isEqualTo(loanId);
        assertThat(z.getLastPublishedLoanInfo()).isNotNull();
    }

    @Test
    void portfolioApi() {
        final PaginatedApi<InvestmentImpl, PortfolioApi> pa = mockApi();
        when(pa.execute(notNull())).thenReturn(mock(InvestmentImpl.class));
        final ApiProvider p = spy(new ApiProvider());
        when(p.portfolio(any())).thenReturn(pa);
        final Zonky z = new Zonky(p, () -> mock(ZonkyApiTokenImpl.class));
        assertThat(z.getInvestment(1)).isNotNull();
        when(pa.execute(notNull())).thenReturn(mock(Statistics.class));
        assertThat(z.getStatistics()).isNotNull();
    }

    @Test
    void participationApi() {
        final PaginatedApi<ParticipationImpl, ParticipationApi> pa = mockApi();
        when(pa.execute(notNull())).thenReturn(mock(ParticipationDetail.class));
        when(pa.execute(notNull(), anyBoolean())).thenReturn(mock(LastPublishedItem.class));
        final ApiProvider p = spy(new ApiProvider());
        when(p.secondaryMarketplace(any())).thenReturn(pa);
        final Zonky z = new Zonky(p, () -> mock(ZonkyApiTokenImpl.class));
        assertThat(z.getParticipationDetail(1)).isNotNull();
        assertThat(z.getLastPublishedParticipationInfo()).isNotNull();
    }

    @Test
    void investmentById() {
        // prepare data
        final LoanImpl loan = new LoanImpl();
        loan.setRating(Rating.A);
        loan.setAmount(Money.from(200));
        loan.setRemainingInvestment(Money.from(200));
        final InvestmentImpl investment = new InvestmentImpl(new InvestmentLoanDataImpl(loan), Money.from(200));
        // prepare api
        final PaginatedApi<InvestmentImpl, PortfolioApi> api = mockApi(singletonList(investment));
        final ApiProvider provider = mockApiProvider();
        when(provider.obtainPaginated(eq(PortfolioApi.class), any(), any())).thenReturn(api);
        final Zonky z = mockZonky(provider);
        // start test
        assertThat(z.getInvestmentByLoanId(investment.getLoan()
            .getId()))
                .hasValueSatisfying(i -> assertThat(i.getLoan()
                    .getId()).isEqualTo(investment.getLoan()
                        .getId()));

    }

    @Test
    void getters() {
        final Zonky z = mockZonky();
        assertSoftly(softly -> {
            softly.assertThat(z.getAvailableLoans(Select.unrestricted()))
                .isEmpty();
            softly.assertThat(z.getInvestments(Select.unrestricted()))
                .isEmpty();
            softly.assertThat(z.getDelinquentInvestments())
                .isEmpty();
            softly.assertThat(z.getSoldInvestments())
                .isEmpty();
            softly.assertThat(z.getAvailableParticipations(Select.unrestricted()))
                .isEmpty();
            softly.assertThat(z.getRestrictions())
                .isNotNull();
        });
    }

    @Test
    void invest() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final PaginatedApi<LoanImpl, LoanApi> la = mockApi();
        final int loanId = 1;
        final Loan loan = mock(LoanImpl.class);
        when(loan.getId()).thenReturn(loanId);
        when(loan.getAmount()).thenReturn(Money.from(200.0));
        when(loan.getRemainingInvestment()).thenReturn(Money.from(200.0));
        when(la.execute(any())).thenReturn(loan);
        final Zonky z = mockZonky(ca, la);
        final Loan l = z.getLoan(loanId);
        assertThat(z.invest(l, 200)).isEqualTo(InvestmentResult.success());
    }

    @Test
    void investFailure() {
        final ControlApi control = mock(ControlApi.class);
        doThrow(new ClientErrorException(Response.Status.FORBIDDEN)).when(control)
            .invest(any());
        final Api<ControlApi> ca = mockApi(control);
        final PaginatedApi<LoanImpl, LoanApi> la = mockApi();
        final LoanImpl loan = new LoanImpl();
        loan.setRating(Rating.A);
        loan.setAmount(Money.from(200));
        loan.setRemainingInvestment(Money.from(200));
        when(la.execute(any())).thenReturn(loan);
        final Zonky z = mockZonky(ca, la);
        final Loan l = z.getLoan(loan.getId());
        final Investment i = new InvestmentImpl(new InvestmentLoanDataImpl(loan), Money.from(200));
        assertThat(z.invest(l, 200)
            .getFailureType())
                .contains(InvestmentFailureType.UNKNOWN);
    }

    @Test
    void purchase() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonkyControl(ca);
        final Participation p = mock(ParticipationImpl.class);
        when(p.getRemainingPrincipal()).thenReturn(Money.from(10));
        when(p.getId()).thenReturn(1L);
        assertThat(z.purchase(p)).isEqualTo(PurchaseResult.success());
    }

    @Test
    void purchaseFailure() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        doThrow(new ClientErrorException(Response.Status.FORBIDDEN)).when(control)
            .purchase(anyLong(), any());
        final Zonky z = mockZonkyControl(ca);
        final Participation p = mock(ParticipationImpl.class);
        when(p.getRemainingPrincipal()).thenReturn(Money.from(10));
        when(p.getId()).thenReturn(1L);
        assertThat(z.purchase(p)
            .getFailureType()).contains(PurchaseFailureType.UNKNOWN);
    }

    @Test
    void sell() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonkyControl(ca);
        final LoanImpl loan = new LoanImpl();
        loan.setRating(Rating.D);
        final InvestmentImpl investment = new InvestmentImpl(new InvestmentLoanDataImpl(loan), Money.from(200));
        investment.setId(1);
        investment.setPrincipal(new AmountsImpl(Money.from(10)));
        final SellInfoImpl sellInfo = new SellInfoImpl(Money.from(10), Money.from(1));
        investment.setSmpSellInfo(sellInfo);
        z.sell(investment);
        verify(control).offer(any());
    }

    @Test
    void cancel() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonkyControl(ca);
        final Investment i = new InvestmentImpl(new InvestmentLoanDataImpl(), Money.from(200));
        z.cancel(i);
        verify(control).cancel(eq(i.getId()));
    }

    @Test
    void accept() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonkyControl(ca);
        final MyReservation mr = mock(MyReservationImpl.class);
        when(mr.getId()).thenReturn(111L);
        final Reservation r = mock(ReservationImpl.class);
        when(r.getMyReservation()).thenReturn(mr);
        z.accept(r);
        verify(control).accept(argThat(rs -> {
            final List<ResolutionRequest> rr = rs.getResolutions();
            return rr.size() == 1 && Objects.equals(rr.get(0)
                .getReservationId(),
                    r.getMyReservation()
                        .getId());
        }));
    }

    @Test
    void reservationPreferences() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonkyControl(ca);
        final ReservationPreferencesImpl r = new ReservationPreferencesImpl();
        z.setReservationPreferences(r);
        verify(control).setReservationPreferences(eq(r));
    }

}
