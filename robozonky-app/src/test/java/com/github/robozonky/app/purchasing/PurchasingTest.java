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
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class PurchasingTest extends AbstractZonkyLeveragingTest {

    private static final PurchaseStrategy NONE_ACCEPTING_STRATEGY = (available, portfolio) -> Stream.empty(),
            ALL_ACCEPTING_STRATEGY = (available, portfolio) -> available.stream().map(d -> d.recommend().get());
    private static final Supplier<Optional<PurchaseStrategy>> ALL_ACCEPTING = () -> Optional.of(ALL_ACCEPTING_STRATEGY),
            NONE_ACCEPTING = () -> Optional.of(NONE_ACCEPTING_STRATEGY);

    private static Zonky mockApi() {
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getLoan(ArgumentMatchers.anyInt()))
                .thenAnswer(invocation -> {
                    final int id = invocation.getArgument(0);
                    return new Loan(id, 200);
                });
        Mockito.when(zonky.getWallet()).thenReturn(new Wallet(BigDecimal.valueOf(10000), BigDecimal.valueOf(9000)));
        return zonky;
    }

    @Test
    public void noStrategy() {
        final Participation mock = Mockito.mock(Participation.class);
        final Purchasing exec = new Purchasing(Optional::empty, null, Duration.ofMinutes(60), true);
        final Portfolio portfolio = Mockito.mock(Portfolio.class);
        Assertions.assertThat(exec.apply(portfolio, Collections.singleton(mock))).isEmpty();
        // check events
        final List<Event> events = this.getNewEvents();
        Assertions.assertThat(events).isEmpty();
    }

    @Test
    public void noneAccepted() {
        final Zonky zonky = mockApi();
        final Participation mock = Mockito.mock(Participation.class);
        Mockito.when(mock.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(250));
        final Purchasing exec = new Purchasing(NONE_ACCEPTING, zonky, Duration.ofMinutes(60), true);
        final Portfolio portfolio = Portfolio.create(zonky)
                .orElseThrow(() -> new AssertionError("Should have been present."));
        Assertions.assertThat(exec.apply(portfolio, Collections.singleton(mock))).isEmpty();
        final List<Event> e = this.getNewEvents();
        Assertions.assertThat(e).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(e).first().isInstanceOf(PurchasingStartedEvent.class);
            softly.assertThat(e).last().isInstanceOf(PurchasingCompletedEvent.class);
        });
    }

    @Test
    public void noItems() {
        final Zonky zonky = mockApi();
        final Purchasing exec =
                new Purchasing(ALL_ACCEPTING, zonky, Duration.ofMinutes(60), true);
        final Portfolio portfolio = Portfolio.create(zonky)
                .orElseThrow(() -> new AssertionError("Should have been present."));
        Assertions.assertThat(exec.apply(portfolio, Collections.emptyList())).isEmpty();
        final List<Event> e = this.getNewEvents();
        Assertions.assertThat(e).isEmpty();
    }
}
