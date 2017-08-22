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

package com.github.robozonky.app.portfolio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.api.notifications.SaleRecommendedEvent;
import com.github.robozonky.api.notifications.SaleRequestedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.notifications.SellingStartedEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.investing.AbstractInvestingTest;
import com.github.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

public class SellingTest extends AbstractInvestingTest {

    private static final SellStrategy ALL_ACCEPTING_STRATEGY =
            (available, portfolio) -> available.stream().map(d -> d.recommend().get());
    private static final SellStrategy NONE_ACCEPTING_STRATEGY =
            (available, portfolio) -> Stream.empty();
    private static final Refreshable<SellStrategy> ALL_ACCEPTING_REFRESHABLE =
            new Refreshable<SellStrategy>() {
                @Override
                protected Supplier<Optional<String>> getLatestSource() {
                    return () -> Optional.of("");
                }

                @Override
                protected Optional<SellStrategy> transform(final String source) {
                    return Optional.of(ALL_ACCEPTING_STRATEGY);
                }
            };
    private static final Refreshable<SellStrategy> NONE_ACCEPTING_REFRESHABLE =
            new Refreshable<SellStrategy>() {
                @Override
                protected Supplier<Optional<String>> getLatestSource() {
                    return () -> Optional.of("");
                }

                @Override
                protected Optional<SellStrategy> transform(final String source) {
                    return Optional.of(NONE_ACCEPTING_STRATEGY);
                }
            };

    static { // no need to have to do this in the tests
        ALL_ACCEPTING_REFRESHABLE.run();
        NONE_ACCEPTING_REFRESHABLE.run();
    }

    private static Zonky mockApi(final Investment... investments) {
        final Zonky zonky = Mockito.mock(Zonky.class);
        Mockito.when(zonky.getWallet()).thenReturn(new Wallet(BigDecimal.TEN, BigDecimal.ZERO));
        Mockito.when(zonky.getStatistics()).thenReturn(new Statistics());
        Mockito.when(zonky.getInvestments()).thenReturn(Stream.of(investments));
        Mockito.when(zonky.getLoan(ArgumentMatchers.anyInt())).thenReturn(Mockito.mock(Loan.class));
        return zonky;
    }

    private static Investment mock() {
        final Investment investment = Mockito.mock(Investment.class);
        Mockito.when(investment.getStatus()).thenReturn(InvestmentStatus.ACTIVE);
        Mockito.when(investment.isOnSmp()).thenReturn(false);
        Mockito.when(investment.isCanBeOffered()).thenReturn(true);
        return investment;
    }

    @Test
    public void noSaleDueToNoStrategy() {
        final Refreshable<SellStrategy> r = Refreshable.createImmutable(null);
        r.run();
        new Selling(r, true).accept(null);
        final List<Event> e = getNewEvents();
        Assertions.assertThat(e).hasSize(0);
    }

    @Test
    public void noSaleDueToNoData() { // no data is inserted into portfolio, therefore nothing happens
        final Zonky zonky = mockApi();
        new Selling(ALL_ACCEPTING_REFRESHABLE, true).accept(zonky);
        final List<Event> e = getNewEvents();
        Assertions.assertThat(e).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(e.get(0)).isInstanceOf(SellingStartedEvent.class);
            softly.assertThat(e.get(1)).isInstanceOf(SellingCompletedEvent.class);
        });
        Mockito.verify(zonky, Mockito.never()).sell(ArgumentMatchers.any());
    }

    @Test
    public void noSaleDueToStrategyForbidding() {
        final Investment i = mock();
        final Zonky zonky = mockApi(i);
        Portfolio.INSTANCE.update(zonky); // load investments
        new Selling(NONE_ACCEPTING_REFRESHABLE, true).accept(zonky);
        final List<Event> e = getNewEvents();
        Assertions.assertThat(e).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(e.get(0)).isInstanceOf(SellingStartedEvent.class);
            softly.assertThat(e.get(1)).isInstanceOf(SellingCompletedEvent.class);
        });
        Mockito.verify(zonky, Mockito.never()).sell(ArgumentMatchers.eq(i));
    }

    private void saleMade(final boolean isDryRun) {
        final Investment i = mock();
        final Zonky zonky = mockApi(i);
        Portfolio.INSTANCE.update(zonky); // load investments
        new Selling(ALL_ACCEPTING_REFRESHABLE, isDryRun).accept(zonky);
        final List<Event> e = getNewEvents();
        Assertions.assertThat(e).hasSize(5);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(e.get(0)).isInstanceOf(SellingStartedEvent.class);
            softly.assertThat(e.get(1)).isInstanceOf(SaleRecommendedEvent.class);
            softly.assertThat(e.get(2)).isInstanceOf(SaleRequestedEvent.class);
            softly.assertThat(e.get(3)).isInstanceOf(SaleOfferedEvent.class);
            softly.assertThat(e.get(4)).isInstanceOf(SellingCompletedEvent.class);
        });
        final VerificationMode m = isDryRun ? Mockito.never() : Mockito.times(1);
        Mockito.verify(i, m).setIsOnSmp(ArgumentMatchers.eq(true));
        Mockito.verify(zonky, m).sell(ArgumentMatchers.eq(i));
    }

    @Test
    public void saleMade() {
        saleMade(false);
    }

    @Test
    public void saleMadeDryRun() {
        saleMade(true);
    }
}
