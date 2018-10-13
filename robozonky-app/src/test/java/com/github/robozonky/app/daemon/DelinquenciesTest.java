/*
 * Copyright 2018 The RoboZonky Project
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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.DevelopmentType;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DelinquenciesTest extends AbstractZonkyLeveragingTest {

    private final static Random RANDOM = new Random(0);

    @Test
    void nop() {
        final Zonky z = harmlessZonky(10_000);
        when(z.getDelinquentInvestments()).thenAnswer(invocation -> Stream.empty());
        final TransactionalPortfolio p = createTransactionalPortfolio(z);
        DelinquencyNotificationPayload.notify(p);
        p.run(); // finish the transaction
        verify(z, atLeastOnce()).getDelinquentInvestments();
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
                .setPaymentStatus(PaymentStatus.DUE)
                .setNextPaymentDate(OffsetDateTime.now().minusDays(1))
                .build();
        // make sure new delinquencies are reported and stored
        final Zonky z = harmlessZonky(10_000);
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        final TransactionalPortfolio p = createTransactionalPortfolio(z);
        DelinquencyNotificationPayload.update(p, Collections.singleton(i), Collections.emptySet(), Collections.emptySet());
        p.run(); // finish the transaction
        assertThat(getEventsRequested()).hasSize(1);
        assertThat(getEventsRequested().get(0)).isInstanceOf(LoanNowDelinquentEvent.class);
    }

    private List<Development> assembleDevelopments(final OffsetDateTime target) {
        final Development before = Development.custom()
                .setDateFrom(target.minusDays(2))
                .setDateTo(target.minusDays(1))
                .setType(DevelopmentType.OTHER)
                .setPublicNote("Before target date.")
                .build();
        final Development after1 = Development.custom()
                .setDateFrom(target.plusDays(1))
                .setDateTo(target.plusDays(2))
                .setType(DevelopmentType.OTHER)
                .setPublicNote("First after target.")
                .build();
        final Development after2 = Development.custom()
                .setDateFrom(target.plusDays(3))
                .setDateTo(target.plusDays(4))
                .setType(DevelopmentType.OTHER)
                .setPublicNote("Second after target.")
                .build();
        return Arrays.asList(before, after1, after2);
    }

    @Test
    void noLongerDelinquentThroughRepayment() {
        final OffsetDateTime delinquencyStart = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        final MyInvestment my = mockMyInvestment();
        final Loan l = Loan.custom()
                .setId(RANDOM.nextInt(10000))
                .setRating(Rating.D)
                .setMyInvestment(my)
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setId(my.getId())
                .setPaymentStatus(PaymentStatus.DUE)
                .setNextPaymentDate(delinquencyStart)
                .build();
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getLoan(eq(l.getId()))).thenReturn(l);
        when(zonky.getInvestment(eq(my.getId()))).thenReturn(Optional.of(i));
        // register delinquence
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i));
        final TransactionalPortfolio p = createTransactionalPortfolio(zonky);
        DelinquencyNotificationPayload.notify(p);
        p.run(); // finish the transaction
        this.readPreexistingEvents(); // ignore events just emitted
        // the investment is no longer delinquent
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.empty());
        final List<Development> developments = assembleDevelopments(delinquencyStart);
        when(zonky.getDevelopments(eq(l.getId()))).thenReturn(developments.stream());
        DelinquencyNotificationPayload.notify(p);
        p.run(); // finish the transaction
        // event is fired; only includes developments after delinquency occured, in reverse order
        assertThat(getEventsRequested())
                .hasSize(1)
                .first().isInstanceOf(LoanNoLongerDelinquentEvent.class);
    }

    @Test
    void noLongerDelinquentThroughDefault() {
        final Loan l = Loan.custom()
                .setId(RANDOM.nextInt(10000))
                .setRating(Rating.D)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setPaymentStatus(PaymentStatus.PAID_OFF)
                .setNextPaymentDate(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID))
                .build();
        // register delinquency
        final Zonky z = harmlessZonky(10_000);
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        final TransactionalPortfolio p = createTransactionalPortfolio(z);
        DelinquencyNotificationPayload.update(p, Collections.emptyList(), new HashSet<>(), new HashSet<>());
        p.run(); // finish the transaction
        this.readPreexistingEvents(); // ignore events just emitted
        // the investment is defaulted
        DelinquencyNotificationPayload.update(p, Collections.singleton(i), new HashSet<>(), new HashSet<>());
        p.run(); // finish the transaction
        assertThat(getEventsRequested())
                .hasSize(1)
                .first().isInstanceOf(LoanDefaultedEvent.class);
    }

}
