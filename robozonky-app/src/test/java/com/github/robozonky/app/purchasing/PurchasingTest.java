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
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.PurchasingCompletedEvent;
import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class PurchasingTest extends AbstractZonkyLeveragingTest {

    private static final PurchaseStrategy NONE_ACCEPTING_STRATEGY = (a, p, r) -> Stream.empty(),
            ALL_ACCEPTING_STRATEGY = (a, p, r) -> a.stream().map(d -> d.recommend().get());
    private static final Supplier<Optional<PurchaseStrategy>> ALL_ACCEPTING = () -> Optional.of(ALL_ACCEPTING_STRATEGY),
            NONE_ACCEPTING = () -> Optional.of(NONE_ACCEPTING_STRATEGY);

    private static Zonky mockApi() {
        final Zonky zonky = mock(Zonky.class);
        when(zonky.getLoan(anyInt()))
                .thenAnswer(invocation -> {
                    final int id = invocation.getArgument(0);
                    return Loan.custom().setId(id).setAmount(200).build();
                });
        when(zonky.getWallet()).thenReturn(new Wallet(BigDecimal.valueOf(10000), BigDecimal.valueOf(9000)));
        return zonky;
    }

    @Test
    void noStrategy() {
        final Participation mock = mock(Participation.class);
        final Purchasing exec = new Purchasing(Optional::empty, null, Duration.ofMinutes(60), true);
        final Portfolio portfolio = mock(Portfolio.class);
        assertThat(exec.apply(portfolio, Collections.singleton(mock))).isEmpty();
        // check events
        final List<Event> events = this.getNewEvents();
        assertThat(events).isEmpty();
    }

    @Test
    void noneAccepted() {
        final Zonky zonky = mockApi();
        final Participation mock = mock(Participation.class);
        when(mock.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(250));
        final Purchasing exec = new Purchasing(NONE_ACCEPTING, mockAuthentication(zonky), Duration.ofMinutes(60), true);
        final Portfolio portfolio = Portfolio.create(zonky);
        assertThat(exec.apply(portfolio, Collections.singleton(mock))).isEmpty();
        final List<Event> e = this.getNewEvents();
        assertThat(e).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(e).first().isInstanceOf(PurchasingStartedEvent.class);
            softly.assertThat(e).last().isInstanceOf(PurchasingCompletedEvent.class);
        });
    }

    @Test
    void noItems() {
        final Zonky zonky = mockApi();
        final Purchasing exec =
                new Purchasing(ALL_ACCEPTING, mockAuthentication(zonky), Duration.ofMinutes(60), true);
        final Portfolio portfolio = Portfolio.create(zonky);
        assertThat(exec.apply(portfolio, Collections.emptyList())).isEmpty();
        final List<Event> e = this.getNewEvents();
        assertThat(e).isEmpty();
    }
}
