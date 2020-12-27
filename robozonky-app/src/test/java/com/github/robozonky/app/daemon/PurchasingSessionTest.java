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

package com.github.robozonky.app.daemon;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.remote.entities.ParticipationImpl;
import com.github.robozonky.internal.tenant.RemotePortfolio;
import com.github.robozonky.test.mock.MockLoanBuilder;

class PurchasingSessionTest extends AbstractZonkyLeveragingTest {

    @Test
    void empty() {
        final Zonky z = harmlessZonky();
        final PowerTenant auth = mockTenant(z);
        final Stream<Participation> i = PurchasingSession.purchase(auth, Stream.empty(), null);
        assertThat(i).isEmpty();
    }

    @Test
    void properReal() {
        final Loan l = new MockLoanBuilder()
            .set(LoanImpl::setAmount, Money.from(200))
            .set(LoanImpl::setInterestRate, Rating.D.getInterestRate())
            .set(LoanImpl::setRemainingInvestment, Money.from(200))
            .set(LoanImpl::setReservedAmount, Money.from(0))
            .set(LoanImpl::setMyInvestment, mockMyInvestment())
            .build();
        final int loanId = l.getId();
        final Participation p = mock(ParticipationImpl.class);
        doReturn(l.getId()).when(p)
            .getLoanId();
        doReturn(l.getInterestRate()).when(p)
            .getInterestRate();
        when(p.getRemainingPrincipal()).thenReturn(Money.from(200));
        final PurchaseStrategy s = mock(PurchaseStrategy.class);
        when(s.recommend(any(), any(), any())).thenReturn(true);
        final Zonky z = harmlessZonky();
        when(z.getLoan(eq(loanId))).thenReturn(l);
        final PowerTenant auth = mockTenant(z, false);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> l);
        final Stream<Participation> i = PurchasingSession.purchase(auth, Stream.of(pd), s);
        assertThat(i).hasSize(1);
        assertThat(getEventsRequested()).hasSize(3);
        verify(z).purchase(eq(p));
        final RemotePortfolio rp = auth.getPortfolio();
        final Rating rating = Rating.findByInterestRate(l.getInterestRate());
        verify(rp).simulateCharge(eq(loanId), eq(rating), any());
        verify(auth).setKnownBalanceUpperBound(eq(Money.from(Integer.MAX_VALUE - 200)));
    }

    @Test
    void failure() {
        final Loan l = new MockLoanBuilder()
            .set(LoanImpl::setAmount, Money.from(200))
            .set(LoanImpl::setInterestRate, Rating.D.getInterestRate())
            .set(LoanImpl::setRemainingInvestment, Money.from(200))
            .set(LoanImpl::setReservedAmount, Money.from(0))
            .set(LoanImpl::setMyInvestment, mockMyInvestment())
            .build();
        final Participation p = mock(ParticipationImpl.class);
        doReturn(l.getId()).when(p)
            .getLoanId();
        when(p.getRemainingPrincipal()).thenReturn(Money.from(200));
        final PurchaseStrategy s = mock(PurchaseStrategy.class);
        when(s.recommend(any(), any(), any())).thenReturn(true);
        final Zonky z = harmlessZonky();
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        final Response response = Response.status(400)
            .entity("INSUFFICIENT_BALANCE")
            .build();
        final ClientErrorException thrown = new BadRequestException(response);
        doThrow(thrown).when(z)
            .purchase(any());
        final PowerTenant auth = mockTenant(z, false);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> l);
        final Stream<Participation> i = PurchasingSession.purchase(auth, Stream.of(pd), s);
        assertThat(i).isEmpty();
        assertThat(auth.getKnownBalanceUpperBound()).isEqualTo(Money.from(199));
    }

    @Test
    void failureDueToTooManyRequests() {
        final Loan l = new MockLoanBuilder()
            .set(LoanImpl::setAmount, Money.from(200))
            .set(LoanImpl::setInterestRate, Rating.D.getInterestRate())
            .set(LoanImpl::setRemainingInvestment, Money.from(200))
            .set(LoanImpl::setReservedAmount, Money.from(0))
            .set(LoanImpl::setMyInvestment, mockMyInvestment())
            .build();
        final Participation p = mock(ParticipationImpl.class);
        doReturn(l.getId()).when(p)
            .getLoanId();
        when(p.getRemainingPrincipal()).thenReturn(Money.from(200));
        final PurchaseStrategy s = mock(PurchaseStrategy.class);
        when(s.recommend(any(), any(), any())).thenReturn(true);
        final Zonky z = harmlessZonky();
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        final Response response = Response.status(400)
            .entity("TOO_MANY_REQUESTS")
            .build();
        final ClientErrorException thrown = new BadRequestException(response);
        doThrow(thrown).when(z)
            .purchase(any());
        final PowerTenant auth = mockTenant(z, false);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> l);
        assertThatThrownBy(() -> PurchasingSession.purchase(auth, Stream.of(pd), s))
            .isInstanceOf(IllegalStateException.class);
    }

}
