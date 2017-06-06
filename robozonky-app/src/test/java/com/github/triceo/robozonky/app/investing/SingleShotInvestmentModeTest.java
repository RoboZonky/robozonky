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

package com.github.triceo.robozonky.app.investing;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.marketplaces.ExpectedTreatment;
import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.triceo.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.triceo.robozonky.api.notifications.LoanArrivedEvent;
import com.github.triceo.robozonky.api.remote.WalletApi;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Wallet;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.common.remote.Apis;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class SingleShotInvestmentModeTest extends AbstractInvestingTest {

    private static final class TestMarketplace implements Marketplace {

        private Consumer<Collection<Loan>> listener;
        private final Collection<Loan> toReturn;
        private boolean closed = false;

        public TestMarketplace(final Collection<Loan> toReturn) {
            this.toReturn = toReturn;
        }

        @Override
        public boolean registerListener(final Consumer<Collection<Loan>> listener) {
            this.listener = listener;
            return true;
        }

        @Override
        public ExpectedTreatment specifyExpectedTreatment() {
            return ExpectedTreatment.POLLING;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void run() {
            this.listener.accept(toReturn);
        }

        @Override
        public void close() throws Exception {
            this.closed = true;
        }
    }

    @Test
    public void wrongMarketplace() {
        final Marketplace m = Mockito.mock(Marketplace.class);
        Mockito.when(m.specifyExpectedTreatment()).thenReturn(ExpectedTreatment.LISTENING);
        Assertions.assertThatThrownBy(() -> new SingleShotInvestmentMode(null, null, false, m, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void standard() {
        final Apis p = harmlessApi();
        final Wallet w = new Wallet(1, 2, BigDecimal.TEN, BigDecimal.ZERO);
        final WalletApi wa = Mockito.mock(WalletApi.class);
        Mockito.when(wa.wallet()).thenReturn(w);
        Mockito.doReturn(new Apis.Wrapper<>(wa)).when(p).wallet(ArgumentMatchers.any());
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getId()).thenReturn(1);
        Mockito.when(l.getAmount()).thenReturn(10000.0);
        Mockito.when(l.getDatePublished()).thenReturn(OffsetDateTime.now());
        Mockito.when(l.getRemainingInvestment()).thenReturn(1000.0);
        final TestMarketplace m = new TestMarketplace(Collections.singletonList(l));
        final Refreshable<InvestmentStrategy> s = Refreshable.createImmutable(Mockito.mock(InvestmentStrategy.class));
        s.run();
        try (final SingleShotInvestmentMode exec = new SingleShotInvestmentMode(
                AuthenticationHandler.passwordBased(SecretProvider.fallback("username", new char[0])),
                new ZonkyProxy.Builder().asDryRun(), true, m, s)) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(exec.execute(p)).isPresent();
                softly.assertThat(exec.isFaultTolerant()).isTrue();
                softly.assertThat(exec.isDryRun()).isTrue();
            });
        } catch (final Exception ex) {
            Assertions.fail("Unexpected exception.", ex);
        } finally {
            Assertions.assertThat(m.isClosed()).isTrue();
        }
        // validate events
        final List<Event> events = this.getNewEvents();
        Assertions.assertThat(events).hasSize(3);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(events.get(0)).isInstanceOf(LoanArrivedEvent.class);
            softly.assertThat(events.get(1)).isInstanceOf(ExecutionStartedEvent.class);
            softly.assertThat(events.get(2)).isInstanceOf(ExecutionCompletedEvent.class);
        });
    }

    @Test
    public void empty() {
        final Apis p = AbstractInvestingTest.harmlessApi();
        final SingleShotInvestmentModeTest.TestMarketplace m = new SingleShotInvestmentModeTest.TestMarketplace(Collections.emptyList());
        try (final SingleShotInvestmentMode exec = new SingleShotInvestmentMode(null, null, true, m, null)) {
            Assertions.assertThat(exec.execute(p)).isPresent();
        } catch (final Exception ex) {
            Assertions.fail("Unexpected exception.", ex);
        } finally {
            Assertions.assertThat(m.isClosed()).isTrue();
        }
        // validate events
        final List<Event> events = this.getNewEvents();
        Assertions.assertThat(events).hasSize(0);
    }

    @Test
    public void failingDuringInvest() {
        final Apis p = AbstractInvestingTest.harmlessApi();
        final Loan l = Mockito.mock(Loan.class);
        Mockito.doThrow(IllegalStateException.class).when(p).wallet(ArgumentMatchers.any());
        Mockito.when(l.getId()).thenReturn(1);
        Mockito.when(l.getAmount()).thenReturn(10000.0);
        Mockito.when(l.getDatePublished()).thenReturn(OffsetDateTime.now());
        Mockito.when(l.getRemainingInvestment()).thenReturn(1000.0);
        final Marketplace m = new SingleShotInvestmentModeTest.TestMarketplace(Collections.singletonList(l));
        final Refreshable<InvestmentStrategy> s = Refreshable.createImmutable(Mockito.mock(InvestmentStrategy.class));
        s.run();
        try (final SingleShotInvestmentMode exec = new SingleShotInvestmentMode(
                AuthenticationHandler.passwordBased(SecretProvider.fallback("username")),
                new ZonkyProxy.Builder(), true, m, s)) {
            Assertions.assertThat(exec.execute(p)).isEmpty();
        } catch (final Exception ex) {
            Assertions.fail("Unexpected exception.", ex);
        }
        // validate execution not started
        Assertions.assertThat(this.getNewEvents()).hasSize(1).first().isInstanceOf(LoanArrivedEvent.class);
    }

}

