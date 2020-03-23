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

package com.github.robozonky.app.delinquencies;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanLostEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;

class DelinquencyNotificationPayloadTest extends AbstractZonkyLeveragingTest {

    private final Zonky zonky = harmlessZonky();
    private final Tenant tenant = mockTenant(zonky);
    private final Registry r = new Registry(tenant);
    private final DelinquencyNotificationPayload payload = new DelinquencyNotificationPayload(t -> r);

    @Test
    void initializesWithoutTriggeringEvents() {
        final Investment i0 = MockInvestmentBuilder.fresh()
            .setLegalDpd(0)
            .build();
        final Investment i1 = MockInvestmentBuilder.fresh()
            .setLegalDpd(1)
            .build();
        final Investment i10 = MockInvestmentBuilder.fresh()
            .setLegalDpd(10)
            .build();
        final Investment i30 = MockInvestmentBuilder.fresh()
            .setLegalDpd(30)
            .build();
        final Investment i60 = MockInvestmentBuilder.fresh()
            .setLegalDpd(60)
            .build();
        final Investment i90 = MockInvestmentBuilder.fresh()
            .setLegalDpd(90)
            .build();
        final Investment defaulted = MockInvestmentBuilder.fresh()
            .setPaymentStatus(PaymentStatus.PAID_OFF)
            .build();
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i0, i1, i10, i30, i60, i90, defaulted));
        // run test
        payload.accept(tenant);
        assertSoftly(softly -> {
            softly.assertThat(r.getCategories(i0))
                .isEmpty();
            softly.assertThat(r.getCategories(i1))
                .containsExactly(Category.NEW);
            softly.assertThat(r.getCategories(i10))
                .containsExactly(Category.NEW, Category.MILD);
            softly.assertThat(r.getCategories(i30))
                .containsExactly(Category.NEW, Category.MILD, Category.SEVERE);
            softly.assertThat(r.getCategories(i60))
                .containsExactly(Category.NEW, Category.MILD, Category.SEVERE,
                        Category.CRITICAL);
            softly.assertThat(r.getCategories(i90))
                .containsExactly(Category.NEW, Category.MILD, Category.SEVERE,
                        Category.CRITICAL, Category.HOPELESS);
            softly.assertThat(r.getCategories(defaulted))
                .containsExactly(Category.NEW, Category.MILD, Category.SEVERE,
                        Category.CRITICAL, Category.HOPELESS,
                        Category.DEFAULTED);
            softly.assertThat(getEventsRequested())
                .isEmpty();
        });
    }

    @Test
    void triggersEventsOnNewDelinquents() {
        final Investment i1 = MockInvestmentBuilder.fresh()
            .setLegalDpd(1)
            .build();
        final Investment i10 = MockInvestmentBuilder.fresh()
            .setLegalDpd(10)
            .build();
        final Investment i30 = MockInvestmentBuilder.fresh()
            .setLegalDpd(30)
            .build();
        final Investment i60 = MockInvestmentBuilder.fresh()
            .setLegalDpd(60)
            .build();
        final Investment i90 = MockInvestmentBuilder.fresh()
            .setLegalDpd(90)
            .build();
        final Investment defaulted = MockInvestmentBuilder.fresh()
            .setPaymentStatus(PaymentStatus.PAID_OFF)
            .build();
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i1));
        // run test
        payload.accept(tenant); // nothing will happen here, as this is the initializing run
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i10, i30, i60, i90, defaulted));
        final Loan fresh = MockLoanBuilder.fresh();
        when(zonky.getLoan(anyInt())).thenReturn(fresh);
        payload.accept(tenant); // the new delinquencies will show up now
        assertThat(getEventsRequested())
            .extracting(e -> (Object) e.getClass()
                .getInterfaces()[0])
            .containsOnly(LoanDefaultedEvent.class,
                    LoanDelinquent10DaysOrMoreEvent.class, LoanDelinquent30DaysOrMoreEvent.class,
                    LoanDelinquent60DaysOrMoreEvent.class, LoanDelinquent90DaysOrMoreEvent.class);
    }

    @Test
    void handlesRegularHealing() {
        final Investment i1 = MockInvestmentBuilder.fresh()
            .setLegalDpd(1)
            .build();
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i1));
        // run test
        payload.accept(tenant); // nothing will happen here, as this is the initializing run
        final Investment i2 = MockInvestmentBuilder.fresh()
            .setLegalDpd(1)
            .setPaymentStatus(PaymentStatus.OK)
            .build();
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i2));
        final Loan fresh = MockLoanBuilder.fresh();
        when(zonky.getLoan(anyInt())).thenReturn(fresh);
        payload.accept(tenant); // the new delinquency will show up now
        assertThat(getEventsRequested()).hasSize(1)
            .extracting(e -> (Object) e.getClass()
                .getInterfaces()[0])
            .containsOnly(LoanNowDelinquentEvent.class);
        readPreexistingEvents();
        // now the same delinquency is no longer available
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.empty());
        when(zonky.getInvestment(eq(i2.getId()))).thenReturn(Optional.of(i2));
        payload.accept(tenant); // the new delinquency will show up now
        assertThat(getEventsRequested()).hasSize(1)
            .first()
            .isInstanceOf(LoanNoLongerDelinquentEvent.class);
    }

    @Test
    void handlesLoss() {
        final Investment i1 = MockInvestmentBuilder.fresh()
            .setLegalDpd(1)
            .build();
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i1));
        // run test
        payload.accept(tenant); // nothing will happen here, as this is the initializing run
        final Investment i2 = MockInvestmentBuilder.fresh()
            .setLegalDpd(1)
            .setPaymentStatus(PaymentStatus.WRITTEN_OFF)
            .build();
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i2));
        final Loan fresh = MockLoanBuilder.fresh();
        when(zonky.getLoan(anyInt())).thenReturn(fresh);
        payload.accept(tenant); // the new delinquency will show up now
        assertThat(getEventsRequested()).hasSize(1)
            .extracting(e -> (Object) e.getClass()
                .getInterfaces()[0])
            .containsOnly(LoanNowDelinquentEvent.class);
        readPreexistingEvents();
        // now the same delinquency is no longer available
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.empty());
        when(zonky.getInvestment(eq(i2.getId()))).thenReturn(Optional.of(i2));
        payload.accept(tenant); // the lost loan will show up now
        assertThat(getEventsRequested()).hasSize(1)
            .first()
            .isInstanceOf(LoanLostEvent.class);
    }

    @Test
    void handlesRepayment() {
        final Investment i1 = MockInvestmentBuilder.fresh()
            .setLegalDpd(1)
            .build();
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i1));
        // run test
        payload.accept(tenant); // nothing will happen here, as this is the initializing run
        final Investment i2 = MockInvestmentBuilder.fresh()
            .setLegalDpd(1)
            .setPaymentStatus(PaymentStatus.PAID)
            .build();
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i2));
        final Loan fresh = MockLoanBuilder.fresh();
        when(zonky.getLoan(anyInt())).thenReturn(fresh);
        payload.accept(tenant); // the new delinquency will show up now
        assertThat(getEventsRequested()).hasSize(1)
            .extracting(e -> (Object) e.getClass()
                .getInterfaces()[0])
            .containsOnly(LoanNowDelinquentEvent.class);
        readPreexistingEvents();
        // now the same delinquency is no longer available
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.empty());
        when(zonky.getInvestment(eq(i2.getId()))).thenReturn(Optional.of(i2));
        payload.accept(tenant); // nothing happens, repayments are not handled by this class
        assertThat(getEventsRequested()).isEmpty();
    }
}
