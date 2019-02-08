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

package com.github.robozonky.notifications.listeners;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;
import com.github.robozonky.notifications.Target;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DelinquencyTrackerTest extends AbstractRoboZonkyTest {

    private static final Loan LOAN = Loan.custom()
            .setId(1)
            .setAmount(200)
            .setAnnuity(BigDecimal.TEN)
            .setRating(Rating.D)
            .setInterestRate(new BigDecimal(Rating.D.getCode()))
            .setPurpose(Purpose.AUTO_MOTO)
            .setRegion(Region.JIHOCESKY)
            .setMainIncomeType(MainIncomeType.EMPLOYMENT)
            .setName("")
            .setUrl(getSomeUrl())
            .build();
    private static final Investment INVESTMENT = Investment.fresh(LOAN, 200)
            .setInvestmentDate(OffsetDateTime.now())
            .build();
    private static final Loan LOAN2 = Loan.custom()
            .setId(2)
            .setAmount(200)
            .setAnnuity(BigDecimal.TEN)
            .setRating(Rating.A)
            .setInterestRate(new BigDecimal(Rating.A.getCode()))
            .setPurpose(Purpose.CESTOVANI)
            .setRegion(Region.JIHOMORAVSKY)
            .setMainIncomeType(MainIncomeType.OTHERS_MAIN)
            .setName("")
            .setUrl(getSomeUrl())
            .build();
    private static final Investment INVESTMENT2 = Investment.fresh(LOAN2, 200)
            .setInvestmentDate(OffsetDateTime.now())
            .build();
    private static SessionInfo SESSION = new SessionInfo("someone@robozonky.cz");

    public static URL getSomeUrl() {
        try {
            return new URL("http://localhost");
        } catch (final MalformedURLException ex) {
            Assertions.fail("Shouldn't have happened.", ex);
            return null;
        }
    }

    @Test
    void standard() {
        final DelinquencyTracker t = new DelinquencyTracker(Target.EMAIL);
        assertThat(t.isDelinquent(SESSION, INVESTMENT)).isFalse();
        t.setDelinquent(SESSION, INVESTMENT);
        t.setDelinquent(SESSION, INVESTMENT2);
        assertThat(t.isDelinquent(SESSION, INVESTMENT)).isTrue();
        assertThat(t.isDelinquent(SESSION, INVESTMENT2)).isTrue();
        t.unsetDelinquent(SESSION, INVESTMENT);
        assertThat(t.isDelinquent(SESSION, INVESTMENT2)).isTrue();
        assertThat(t.isDelinquent(SESSION, INVESTMENT)).isFalse();
    }

    @Test
    void notifying() throws Exception {
        final AbstractTargetHandler h = AbstractListenerTest.getHandler();
        final EventListener<LoanDelinquentEvent> l =
                new LoanDelinquentEventListener(SupportedListener.LOAN_DELINQUENT_10_PLUS, h);
        final EventListener<LoanNoLongerDelinquentEvent> l2 =
                new LoanNoLongerDelinquentEventListener(SupportedListener.LOAN_NO_LONGER_DELINQUENT, h);
        final LoanNoLongerDelinquentEvent evt = new MyLoanNoLongerDelinquentEvent();
        l2.handle(evt, SESSION);
        verify(h, never()).send(any(), any(), any(), any()); // not delinquent before, not sending
        l.handle(new MyLoanDelinquent10DaysOrMoreEvent(), SESSION);
        verify(h).send(eq(SESSION), any(), any(), any());
        l2.handle(evt, SESSION);
        verify(h, times(2)).send(eq(SESSION), any(), any(), any()); // delinquency now registered, send
        l2.handle(evt, SESSION);
        verify(h, times(2)).send(eq(SESSION), any(), any(), any()); // already unregistered, send
    }

    private static class MyLoanDelinquent10DaysOrMoreEvent implements LoanDelinquent10DaysOrMoreEvent {

        @Override
        public LocalDate getDelinquentSince() {
            return LocalDate.now();
        }

        @Override
        public Collection<Development> getCollectionActions() {
            return Collections.emptyList();
        }

        @Override
        public Investment getInvestment() {
            return INVESTMENT;
        }

        @Override
        public Loan getLoan() {
            return LOAN;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public int getThresholdInDays() {
            return 10;
        }
    }

    private static class MyLoanNoLongerDelinquentEvent implements LoanNoLongerDelinquentEvent {

        @Override
        public Investment getInvestment() {
            return INVESTMENT;
        }

        @Override
        public Loan getLoan() {
            return LOAN;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }
    }
}
