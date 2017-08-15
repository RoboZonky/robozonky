/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.purchasing;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.triceo.robozonky.api.notifications.PurchaseRecommendedEvent;
import com.github.triceo.robozonky.api.notifications.PurchaseRequestedEvent;
import com.github.triceo.robozonky.api.notifications.PurchasingCompletedEvent;
import com.github.triceo.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Participation;
import com.github.triceo.robozonky.api.remote.entities.Statistics;
import com.github.triceo.robozonky.api.remote.entities.Wallet;
import com.github.triceo.robozonky.api.strategies.ParticipationDescriptor;
import com.github.triceo.robozonky.api.strategies.PurchaseStrategy;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.investing.AbstractInvestingTest;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class StrategyExecutionTest extends AbstractInvestingTest {

    private static final PurchaseStrategy ALL_ACCEPTING_STRATEGY =
            (available, portfolio) -> available.stream().map(d -> d.recommend().get());
    private static final Refreshable<PurchaseStrategy> ALL_ACCEPTING_REFRESHABLE =
            new Refreshable<PurchaseStrategy>() {
                @Override
                protected Supplier<Optional<String>> getLatestSource() {
                    return () -> Optional.of("");
                }

                @Override
                protected Optional<PurchaseStrategy> transform(final String source) {
                    return Optional.of(ALL_ACCEPTING_STRATEGY);
                }
            };
    private static final PurchaseStrategy NONE_ACCEPTING_STRATEGY =
            (available, portfolio) -> Stream.empty();
    private static final Refreshable<PurchaseStrategy> NONE_ACCEPTING_REFRESHABLE =
            new Refreshable<PurchaseStrategy>() {
                @Override
                protected Supplier<Optional<String>> getLatestSource() {
                    return () -> Optional.of("");
                }

                @Override
                protected Optional<PurchaseStrategy> transform(final String source) {
                    return Optional.of(NONE_ACCEPTING_STRATEGY);
                }
            };

    static {
        ALL_ACCEPTING_REFRESHABLE.run();
        NONE_ACCEPTING_REFRESHABLE.run();
    }

    @Test
    public void noStrategy() {
        final Participation mock = Mockito.mock(Participation.class);
        final ParticipationDescriptor pd = new ParticipationDescriptor(mock, null);
        final Refreshable<PurchaseStrategy> r = Refreshable.createImmutable(null);
        r.run();
        final StrategyExecution exec = new StrategyExecution(r, null, Duration.ofMinutes(60), true);
        Assertions.assertThat(exec.apply(Collections.singletonList(pd))).isEmpty();
        // check events
        final List<Event> events = this.getNewEvents();
        Assertions.assertThat(events).isEmpty();
    }

    private static Zonky mockApi() {
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getWallet()).thenReturn(new Wallet(BigDecimal.valueOf(10000), BigDecimal.valueOf(9000)));
        Mockito.when(zonky.getStatistics()).thenReturn(new Statistics());
        return zonky;
    }

    @Test
    public void someItems() {
        final Authenticated auth = Mockito.mock(Authenticated.class);
        Mockito.when(auth.call(ArgumentMatchers.isNotNull())).thenAnswer(invocation -> {
            final Function<Zonky, Collection<Investment>> f = invocation.getArgument(0);
            return f.apply(mockApi());
        });
        final Participation mock = Mockito.mock(Participation.class);
        Mockito.when(mock.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(250));
        final ParticipationDescriptor pd = new ParticipationDescriptor(mock, null);
        final StrategyExecution exec = new StrategyExecution(ALL_ACCEPTING_REFRESHABLE, auth, Duration.ofMinutes(60),
                                                             true);
        Assertions.assertThat(exec.apply(Collections.singletonList(pd))).isNotEmpty();
        final List<Event> e = this.getNewEvents();
        Assertions.assertThat(e).hasSize(5);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(e.get(0)).isInstanceOf(PurchasingStartedEvent.class);
            softly.assertThat(e.get(1)).isInstanceOf(PurchaseRecommendedEvent.class);
            softly.assertThat(e.get(2)).isInstanceOf(PurchaseRequestedEvent.class);
            softly.assertThat(e.get(3)).isInstanceOf(InvestmentPurchasedEvent.class);
            softly.assertThat(e.get(4)).isInstanceOf(PurchasingCompletedEvent.class);
        });
    }

    @Test
    public void noneAccepted() {
        final Authenticated auth = Mockito.mock(Authenticated.class);
        Mockito.when(auth.call(ArgumentMatchers.isNotNull())).thenAnswer(invocation -> {
            final Function<Zonky, Collection<Investment>> f = invocation.getArgument(0);
            return f.apply(mockApi());
        });
        final Participation mock = Mockito.mock(Participation.class);
        Mockito.when(mock.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(250));
        final ParticipationDescriptor pd = new ParticipationDescriptor(mock, null);
        final StrategyExecution exec = new StrategyExecution(NONE_ACCEPTING_REFRESHABLE, auth, Duration.ofMinutes(60),
                                                             true);
        Assertions.assertThat(exec.apply(Collections.singletonList(pd))).isEmpty();
        final List<Event> e = this.getNewEvents();
        Assertions.assertThat(e).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(e.get(0)).isInstanceOf(PurchasingStartedEvent.class);
            softly.assertThat(e.get(1)).isInstanceOf(PurchasingCompletedEvent.class);
        });
    }

    @Test
    public void noItems() {
        final Authenticated auth = Mockito.mock(Authenticated.class);
        Mockito.when(auth.call(ArgumentMatchers.isNotNull())).thenAnswer(invocation -> {
            final Function<Zonky, Collection<Investment>> f = invocation.getArgument(0);
            return f.apply(mockApi());
        });
        final StrategyExecution exec = new StrategyExecution(ALL_ACCEPTING_REFRESHABLE, auth, Duration.ofMinutes(60),
                                                             true);
        Assertions.assertThat(exec.apply(Collections.emptyList())).isEmpty();
        final List<Event> e = this.getNewEvents();
        Assertions.assertThat(e).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(e.get(0)).isInstanceOf(PurchasingStartedEvent.class);
            softly.assertThat(e.get(1)).isInstanceOf(PurchasingCompletedEvent.class);
        });
    }
}
