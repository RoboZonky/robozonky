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

package com.github.robozonky.app.purchasing;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.PurchaseRequestedEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class SessionTest extends AbstractZonkyLeveragingTest {

    private static Zonky mockZonky() {
        return mockZonky(BigDecimal.ZERO);
    }

    private static Zonky mockZonky(final BigDecimal balance) {
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getWallet()).thenReturn(new Wallet(balance, balance));
        return zonky;
    }

    @Test
    public void empty() {
        final Zonky z = mockZonky();
        final Portfolio portfolio = Portfolio.create(z)
                .orElseThrow(() -> new AssertionError("Should have been present,"));
        final Collection<Investment> i = Session.purchase(portfolio, z, Collections.emptyList(), null, true);
        Assertions.assertThat(i).isEmpty();
    }

    @Test
    public void underBalance() {
        final Participation p = Mockito.mock(Participation.class);
        Mockito.when(p.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(200));
        final Loan l = Mockito.mock(Loan.class);
        final PurchaseStrategy s = Mockito.mock(PurchaseStrategy.class);
        Mockito.when(s.recommend(ArgumentMatchers.any(), ArgumentMatchers.any())).thenAnswer(i -> {
            final Collection<ParticipationDescriptor> participations = i.getArgument(0);
            return participations.stream()
                    .map(ParticipationDescriptor::recommend)
                    .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
        });
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, l);
        final Zonky z = mockZonky();
        final Portfolio portfolio = Portfolio.create(z)
                .orElseThrow(() -> new AssertionError("Should have been present,"));
        final Collection<Investment> i = Session.purchase(portfolio, z, Collections.singleton(pd), s, true);
        Assertions.assertThat(i).isEmpty();
        Assertions.assertThat(this.getNewEvents()).has(new Condition<List<? extends Event>>() {
            @Override
            public boolean matches(final List<? extends Event> events) {
                return events.stream().noneMatch(e -> e instanceof PurchaseRequestedEvent);
            }
        });
    }

    @Test
    public void properDryRun() {
        final Loan l = new Loan(1, 200);
        final Participation p = Mockito.mock(Participation.class);
        Mockito.when(p.getLoanId()).thenReturn(l.getId());
        Mockito.when(p.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(200));
        final PurchaseStrategy s = Mockito.mock(PurchaseStrategy.class);
        Mockito.when(s.recommend(ArgumentMatchers.any(), ArgumentMatchers.any())).thenAnswer(i -> {
            final Collection<ParticipationDescriptor> participations = i.getArgument(0);
            return participations.stream()
                    .map(ParticipationDescriptor::recommend)
                    .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
        });
        final Zonky zonky = mockZonky(BigDecimal.valueOf(100_000));
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(l.getId()))).thenReturn(l);
        final Portfolio portfolio = Mockito.spy(Portfolio.create(zonky)
                                                        .orElseThrow(
                                                                () -> new AssertionError("Should have been present,")));
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, l);
        final Collection<Investment> i = Session.purchase(portfolio, zonky, Collections.singleton(pd), s, true);
        Assertions.assertThat(i).hasSize(1);
        Assertions.assertThat(this.getNewEvents()).hasSize(5);
        Mockito.verify(zonky, Mockito.never()).purchase(ArgumentMatchers.eq(p));
        Mockito.verify(portfolio).newBlockedAmount(ArgumentMatchers.eq(zonky),
                                                   ArgumentMatchers.argThat((a) -> a.getLoanId() == l.getId()));
    }

    @Test
    public void properReal() {
        final Loan l = new Loan(1, 200);
        final Participation p = Mockito.mock(Participation.class);
        Mockito.when(p.getLoanId()).thenReturn(l.getId());
        Mockito.when(p.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(200));
        final PurchaseStrategy s = Mockito.mock(PurchaseStrategy.class);
        Mockito.when(s.recommend(ArgumentMatchers.any(), ArgumentMatchers.any())).thenAnswer(i -> {
            final Collection<ParticipationDescriptor> participations = i.getArgument(0);
            return participations.stream()
                    .map(ParticipationDescriptor::recommend)
                    .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
        });
        final Zonky zonky = mockZonky(BigDecimal.valueOf(100_000));
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(l.getId()))).thenReturn(l);
        final Portfolio portfolio = Mockito.spy(Portfolio.create(zonky)
                                                        .orElseThrow(
                                                                () -> new AssertionError("Should have been present,")));
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, l);
        final Collection<Investment> i = Session.purchase(portfolio, zonky, Collections.singleton(pd), s, false);
        Assertions.assertThat(i).hasSize(1);
        Assertions.assertThat(this.getNewEvents()).hasSize(5);
        Mockito.verify(zonky).purchase(ArgumentMatchers.eq(p));
        Mockito.verify(portfolio).newBlockedAmount(ArgumentMatchers.eq(zonky),
                                                   ArgumentMatchers.argThat((a) -> a.getLoanId() == l.getId()));
    }
}
