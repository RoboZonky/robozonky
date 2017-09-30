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
import com.github.robozonky.app.investing.AbstractInvestingTest;
import com.github.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class SessionTest extends AbstractInvestingTest {

    private static Zonky mockZonky() {
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getWallet()).thenReturn(new Wallet(BigDecimal.ZERO, BigDecimal.ZERO));
        return zonky;
    }

    @Test
    public void empty() {
        final Collection<Investment> i = Session.purchase(mockZonky(), Collections.emptyList(), null, true);
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
        final InvestmentCommand c = new InvestmentCommand(s);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, l);
        final Collection<Investment> i = Session.purchase(mockZonky(), Collections.singleton(pd), c, true);
        Assertions.assertThat(i).isEmpty();
        Assertions.assertThat(this.getNewEvents()).has(new Condition<List<? extends Event>>() {
            @Override
            public boolean matches(final List<? extends Event> events) {
                return events.stream().noneMatch(e -> e instanceof PurchaseRequestedEvent);
            }
        });
    }
}
