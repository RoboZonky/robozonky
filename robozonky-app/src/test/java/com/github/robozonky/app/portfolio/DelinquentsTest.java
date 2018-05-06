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
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.DevelopmentTpe;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

class DelinquentsTest extends AbstractZonkyLeveragingTest {

    private static final BiFunction<Loan, LocalDate, Collection<Development>> COLLECTIONS_SUPPLIER =
            (l, s) -> Collections.emptyList();
    private final static Random RANDOM = new Random(0);

    @Test
    void empty() {
        final Tenant t = mockTenant();
        assertThat(Delinquents.getDelinquents(t)).isEmpty();
        assertThat(this.getNewEvents()).isEmpty();
    }

    @Test
    void nop() {
        final Zonky z = harmlessZonky(10_000);
        when(z.getInvestments((Select) any())).thenAnswer(invocation -> Stream.empty());
        final Tenant a = mockTenant(z);
        Delinquents.update(a, Portfolio.create(a, mockBalance(z)));
        verify(z, atLeastOnce()).getInvestments((Select) any());
    }

    @Test
    void newDelinquence() {
        final Loan l = Loan.custom()
                .setId(RANDOM.nextInt(10000))
                .setRating(Rating.D)
                .setAmount(200)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setNextPaymentDate(OffsetDateTime.now().minusDays(1))
                .build();
        final Function<Integer, Loan> f = (id) -> l;
        final Function<Loan, Investment> lif = (loan) -> i;
        // make sure new delinquencies are reported and stored
        final Tenant t = mockTenant();
        Delinquents.update(t, Collections.singleton(i), lif, f, COLLECTIONS_SUPPLIER);
        assertSoftly(softly -> {
            softly.assertThat(Delinquents.getDelinquents(t)).hasSize(1);
            softly.assertThat(this.getNewEvents()).hasSize(1);
        });
        assertThat(this.getNewEvents().get(0)).isInstanceOf(LoanNowDelinquentEvent.class);
        // make sure delinquencies are persisted even when there are none present
        Delinquents.update(t, Collections.emptyList(), lif, f, COLLECTIONS_SUPPLIER);
        assertSoftly(softly -> {
            softly.assertThat(Delinquents.getDelinquents(t)).hasSize(1);
            softly.assertThat(this.getNewEvents()).hasSize(2);
        });
        assertThat(this.getNewEvents().get(1)).isInstanceOf(LoanNoLongerDelinquentEvent.class);
        // and when they are no longer active, they're gone for good
        Delinquents.update(t, Collections.emptyList(), lif, f, COLLECTIONS_SUPPLIER);
        assertThat(Delinquents.getDelinquents(t)).hasSize(0);
    }

    @Test
    void oldDelinquency() {
        final Loan l = Loan.custom()
                .setId(RANDOM.nextInt(10000))
                .setRating(Rating.D)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setNextPaymentDate(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID))
                .build();
        final Function<Integer, Loan> f = (id) -> l;
        final Function<Loan, Investment> lif = (loan) -> i;
        // make sure new delinquencies are reported and stored
        final Tenant t = mockTenant();
        Delinquents.update(t, Collections.singleton(i), lif, f, COLLECTIONS_SUPPLIER);
        assertSoftly(softly -> {
            softly.assertThat(Delinquents.getDelinquents(t)).hasSize(1);
            softly.assertThat(this.getNewEvents()).hasSize(5); // all 5 delinquency events
        });
    }

    private List<Development> assembleDevelopments(final OffsetDateTime target) {
        final Development before = Development.custom()
                .setDateFrom(target.minusDays(2))
                .setDateTo(target.minusDays(1))
                .setType(DevelopmentTpe.OTHER)
                .setPublicNote("Before target date.")
                .build();
        final Development after1 = Development.custom()
                .setDateFrom(target.plusDays(1))
                .setDateTo(target.plusDays(2))
                .setType(DevelopmentTpe.OTHER)
                .setPublicNote("First after target.")
                .build();
        final Development after2 = Development.custom()
                .setDateFrom(target.plusDays(3))
                .setDateTo(target.plusDays(4))
                .setType(DevelopmentTpe.OTHER)
                .setPublicNote("Second after target.")
                .build();
        return Arrays.asList(before, after1, after2);
    }

    @Test
    void noLongerDelinquent() {
        final OffsetDateTime delinquencyStart = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        final Loan l = Loan.custom()
                .setId(RANDOM.nextInt(10000))
                .setRating(Rating.D)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setNextPaymentDate(delinquencyStart)
                .build();
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getLoan(eq(l.getId()))).thenReturn(l);
        final Tenant auth = mockTenant(zonky);
        final Portfolio portfolio = mock(Portfolio.class);
        when(portfolio.lookupOrFail(eq(l), eq(auth))).thenReturn(i);
        // register delinquence
        when(zonky.getInvestments(any())).thenReturn(Stream.of(i));
        Delinquents.update(auth, portfolio);
        this.readPreexistingEvents(); // ignore events just emitted
        // the investment is no longer delinquent
        when(zonky.getInvestments(any())).thenReturn(Stream.empty());
        final List<Development> developments = assembleDevelopments(delinquencyStart);
        when(zonky.getDevelopments(eq(l))).thenReturn(developments.stream());
        Delinquents.update(auth, portfolio);
        // event is fired; only includes developments after delinquency occured, in reverse order
        assertThat(this.getNewEvents()).hasSize(1).first().isInstanceOf(LoanNoLongerDelinquentEvent.class);
        final LoanNoLongerDelinquentEvent e = (LoanNoLongerDelinquentEvent) this.getNewEvents().get(0);
        assertThat(e.getCollectionActions())
                .hasSize(2)
                .first().isEqualTo(developments.get(developments.size() - 1));
    }

    @Test
    void defaulted() {
        final Loan l = Loan.custom()
                .setId(RANDOM.nextInt(10000))
                .setRating(Rating.D)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setPaymentStatus(PaymentStatus.PAID_OFF)
                .setNextPaymentDate(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID))
                .build();
        final Function<Integer, Loan> f = (id) -> l;
        final Function<Loan, Investment> lif = (loan) -> i;
        // register delinquency
        final Tenant t = mockTenant();
        Delinquents.update(t, Collections.singleton(i), lif, f, COLLECTIONS_SUPPLIER);
        this.readPreexistingEvents(); // ignore events just emitted
        // the investment is defaulted
        final BiFunction<Loan, LocalDate, Collection<Development>> f2 = mock(BiFunction.class);
        when(f2.apply(any(), any())).thenReturn(Collections.emptyList());
        Delinquents.update(t, Collections.emptyList(), lif, f, f2);
        assertThat(this.getNewEvents()).hasSize(1).first().isInstanceOf(LoanDefaultedEvent.class);
        verify(f2).apply(any(), any());
    }

    @Test
    void paid() {
        final Loan l = Loan.custom()
                .setId(RANDOM.nextInt(10000))
                .setRating(Rating.D)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setPaymentStatus(PaymentStatus.PAID)
                .setNextPaymentDate(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID))
                .build();
        final Function<Integer, Loan> f = (id) -> l;
        final Function<Loan, Investment> lif = (loan) -> i;
        // register delinquence
        final Tenant t = mockTenant();
        Delinquents.update(t, Collections.singleton(i), lif, f, COLLECTIONS_SUPPLIER);
        assertThat(Delinquents.getAmountsAtRisk()).containsEntry(Rating.D, BigDecimal.valueOf(200));
        this.readPreexistingEvents(); // ignore events just emitted
        // the investment is paid
        Delinquents.update(t, Collections.emptyList(), lif, f, COLLECTIONS_SUPPLIER);
        assertThat(Delinquents.getAmountsAtRisk()).isEmpty();
        assertThat(this.getNewEvents()).isEmpty();
    }

    @Test
    void defaultUpdateTime() {
        final Tenant t = mockTenant();
        assertThat(Delinquents.getLastUpdateTimestamp(t)).isBefore(OffsetDateTime.now());
    }
}
