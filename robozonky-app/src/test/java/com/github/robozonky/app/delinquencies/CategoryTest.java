/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.app.tenant.TransactionalPowerTenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.robozonky.app.delinquencies.Category.CRITICAL;
import static com.github.robozonky.app.delinquencies.Category.DEFAULTED;
import static com.github.robozonky.app.delinquencies.Category.HOPELESS;
import static com.github.robozonky.app.delinquencies.Category.MILD;
import static com.github.robozonky.app.delinquencies.Category.NEW;
import static com.github.robozonky.app.delinquencies.Category.SEVERE;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CategoryTest extends AbstractZonkyLeveragingTest {

    private final Zonky zonky = harmlessZonky(10_000);
    private final Loan loan = Loan.custom().build();
    private final Investment investment = Investment.fresh(loan, 200).build();

    @Test
    void thresholds() {
        assertSoftly(softly -> {
            softly.assertThat(NEW.getThresholdInDays()).isEqualTo(0);
            softly.assertThat(MILD.getThresholdInDays()).isEqualTo(10);
            softly.assertThat(SEVERE.getThresholdInDays()).isEqualTo(30);
            softly.assertThat(CRITICAL.getThresholdInDays()).isEqualTo(60);
            softly.assertThat(HOPELESS.getThresholdInDays()).isEqualTo(90);
        });
    }

    @Test
    void lessers() {
        assertSoftly(softly -> {
            softly.assertThat(NEW.getLesser()).isEmpty();
            softly.assertThat(MILD.getLesser()).containsOnly(NEW);
            softly.assertThat(SEVERE.getLesser()).containsExactly(NEW, MILD);
            softly.assertThat(CRITICAL.getLesser()).containsExactly(NEW, MILD, SEVERE);
            softly.assertThat(HOPELESS.getLesser()).containsExactly(NEW, MILD, SEVERE, CRITICAL);
            softly.assertThat(DEFAULTED.getLesser()).isEmpty();
        });
    }

    @BeforeEach
    void prepareZonky() {
        when(zonky.getLoan(eq(loan.getId()))).thenReturn(loan);
    }

    @Test
    void processNew() {
        final TransactionalPowerTenant transactional = PowerTenant.transactional(mockTenant(zonky));
        NEW.process(transactional, investment);
        transactional.commit();
        assertThat(getEventsRequested()).hasSize(1)
                .first().isInstanceOf(LoanNowDelinquentEvent.class);
        verify(zonky).getLoan(eq(loan.getId()));
        verify(zonky).getDevelopments(eq(loan.getId()));
    }

    @Test
    void processMild() {
        final TransactionalPowerTenant transactional = PowerTenant.transactional(mockTenant(zonky));
        MILD.process(transactional, investment);
        transactional.commit();
        assertThat(getEventsRequested()).hasSize(1)
                .first().isInstanceOf(LoanDelinquent10DaysOrMoreEvent.class);
        verify(zonky).getLoan(eq(loan.getId()));
        verify(zonky).getDevelopments(eq(loan.getId()));
    }

    @Test
    void processSevere() {
        final TransactionalPowerTenant transactional = PowerTenant.transactional(mockTenant(zonky));
        SEVERE.process(transactional, investment);
        transactional.commit();
        assertThat(getEventsRequested()).hasSize(1)
                .first().isInstanceOf(LoanDelinquent30DaysOrMoreEvent.class);
        verify(zonky).getLoan(eq(loan.getId()));
        verify(zonky).getDevelopments(eq(loan.getId()));
    }

    @Test
    void processCritical() {
        final TransactionalPowerTenant transactional = PowerTenant.transactional(mockTenant(zonky));
        CRITICAL.process(transactional, investment);
        transactional.commit();
        assertThat(getEventsRequested()).hasSize(1)
                .first().isInstanceOf(LoanDelinquent60DaysOrMoreEvent.class);
        verify(zonky).getLoan(eq(loan.getId()));
        verify(zonky).getDevelopments(eq(loan.getId()));
    }

    @Test
    void processHopeless() {
        final TransactionalPowerTenant transactional = PowerTenant.transactional(mockTenant(zonky));
        System.out.println("Processing.");
        HOPELESS.process(transactional, investment);
        System.out.println("Processed.");
        transactional.commit();
        System.out.println("Committed.");
        assertThat(getEventsRequested()).hasSize(1)
                .first().isInstanceOf(LoanDelinquent90DaysOrMoreEvent.class);
        verify(zonky).getLoan(eq(loan.getId()));
        verify(zonky).getDevelopments(eq(loan.getId()));
    }

    @Test
    void processDefaulted() {
        final TransactionalPowerTenant transactional = PowerTenant.transactional(mockTenant(zonky));
        DEFAULTED.process(transactional, investment);
        transactional.commit();
        assertThat(getEventsRequested()).hasSize(1)
                .first().isInstanceOf(LoanDefaultedEvent.class);
        verify(zonky).getLoan(eq(loan.getId()));
        verify(zonky).getDevelopments(eq(loan.getId()));
    }
}
