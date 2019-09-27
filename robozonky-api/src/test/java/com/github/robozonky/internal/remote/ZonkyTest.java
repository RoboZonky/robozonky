/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.remote.*;
import com.github.robozonky.api.remote.entities.*;
import com.github.robozonky.internal.Defaults;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class ZonkyTest {

    private static <T, S> PaginatedApi<T, S> mockApi() {
        return mockApi(Collections.emptyList());
    }

    private static final Investment mockInvestment(final Loan loan, final int amount) {
        final Investment i = mock(Investment.class);
        doReturn(loan.getId()).when(i).getLoanId();
        doReturn(loan.getCurrency()).when(i).getCurrency();
        when(i.getAmount()).thenReturn(BigDecimal.valueOf(amount));
        return i;
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
        final Api<ControlApi> ca = ApiProvider.actuallyObtainNormal(mock(ControlApi.class), null);
        doReturn(ca).when(apiProvider).obtainNormal(eq(ControlApi.class), any());
        mockPaginated(apiProvider, LoanApi.class);
        mockPaginated(apiProvider, PortfolioApi.class);
        mockPaginated(apiProvider, ParticipationApi.class);
        mockPaginated(apiProvider, CollectionsApi.class);
        return apiProvider;
    }

    private static Zonky mockZonky(final Api<ControlApi> ca, final PaginatedApi<Loan, LoanApi> la) {
        final ApiProvider apiProvider = mockApiProvider();
        doReturn(ca).when(apiProvider).obtainNormal(eq(ControlApi.class), any());
        mockPaginated(apiProvider, LoanApi.class, la);
        return new Zonky(apiProvider, () -> mock(ZonkyApiToken.class));
    }

    private static Zonky mockZonky() {
        final ApiProvider apiProvider = mockApiProvider();
        return new Zonky(apiProvider, () -> mock(ZonkyApiToken.class));
    }

    private static Zonky mockZonky(final ApiProvider apiProvider) {
        return new Zonky(apiProvider, () -> mock(ZonkyApiToken.class));
    }

    @Test
    void loan() {
        final PaginatedApi<Loan, LoanApi> la = mockApi();
        final int loanId = 1;
        final Loan loan = mock(Loan.class);
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
    void investmentById() {
        // prepare data
        final Loan l1 = mock(Loan.class);
        when(l1.getId()).thenReturn(1);
        when(l1.getCurrency()).thenReturn(Defaults.CURRENCY);
        final Loan l2 = mock(Loan.class);
        when(l2.getId()).thenReturn(2);
        final Investment i1 = mockInvestment(l1, 200);
        final Investment i2 = mockInvestment(l2, 200);
        // prepare api
        final PaginatedApi<Investment, PortfolioApi> api = mockApi(List.of(i2, i1));
        final ApiProvider provider = mockApiProvider();
        when(provider.obtainPaginated(eq(PortfolioApi.class), any(), any())).thenReturn(api);
        final Zonky z = mockZonky(provider);
        // start test
        assertThat(z.getInvestmentByLoanId(i1.getLoanId()))
                .hasValueSatisfying(i -> assertThat(i.getLoanId()).isEqualTo(i1.getLoanId()));

    }

    @Test
    void getters() {
        final Zonky z = mockZonky();
        assertSoftly(softly -> {
            softly.assertThat(z.getAvailableLoans(Select.unrestricted())).isEmpty();
            softly.assertThat(z.getInvestments(Select.unrestricted())).isEmpty();
            softly.assertThat(z.getInvestment(1)).isEmpty();
            softly.assertThat(z.getDelinquentInvestments()).isEmpty();
            softly.assertThat(z.getAvailableParticipations(Select.unrestricted())).isEmpty();
            softly.assertThat(z.getDevelopments(1)).isEmpty();
        });
    }

    @Test
    void investAndlogout() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final PaginatedApi<Loan, LoanApi> la = mockApi();
        final int loanId = 1;
        final Loan loan = mock(Loan.class);
        when(loan.getId()).thenReturn(loanId);
        when(loan.getAmount()).thenReturn(200.0);
        when(loan.getRemainingInvestment()).thenReturn(200.0);
        when(la.execute(any())).thenReturn(loan);
        final Zonky z = mockZonky(ca, la);
        final Loan l = z.getLoan(loanId);
        final Investment i = mockInvestment(l, 200);
        z.invest(i);
        z.logout();
        verify(control, times(1)).invest(any());
        verify(control, times(1)).logout();
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
        final Investment p = mock(Investment.class);
        when(p.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
        when(p.getSmpFee()).thenReturn(Optional.of(BigDecimal.ONE));
        when(p.getId()).thenReturn(1L);
        z.sell(p);
        verify(control).offer(any());
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

    @Test
    void accept() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonkyControl(ca);
        final MyReservation mr = mock(MyReservation.class);
        when(mr.getId()).thenReturn(111L);
        final Reservation r = mock(Reservation.class);
        when(r.getMyReservation()).thenReturn(mr);
        z.accept(r);
        verify(control).accept(argThat(rs -> {
            final List<ResolutionRequest> rr = rs.getResolutions();
            return rr.size() == 1 && Objects.equals(rr.get(0).getReservationId(), r.getMyReservation().getId());
        }));
    }

    @Test
    void reservationPreferences() {
        final ControlApi control = mock(ControlApi.class);
        final Api<ControlApi> ca = mockApi(control);
        final Zonky z = mockZonkyControl(ca);
        final ReservationPreferences r = new ReservationPreferences();
        z.setReservationPreferences(r);
        verify(control).setReservationPreferences(eq(r));
    }

}
