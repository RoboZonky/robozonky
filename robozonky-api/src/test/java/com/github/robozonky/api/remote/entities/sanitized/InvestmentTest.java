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

package com.github.robozonky.api.remote.entities.sanitized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvestmentTest {

    @Mock
    private RawInvestment mocked;

    @Test
    @DisplayName("Sanitization works.")
    void sanitizing() {
        assertThat(Investment.sanitized(mocked, (i) -> LocalDate.now())).isNotNull();
    }

    @Test
    void hasToString() {
        assertThat(Investment.custom().build().toString()).isNotEmpty();
    }

    @Test
    @DisplayName("Custom investment works.")
    void custom() {
        final Investment i = Investment.custom().build();
        assertThat(i).isNotNull();
    }

    @Test
    @DisplayName("Fresh from Loan works.")
    void freshFromLoan() {
        final Loan l = Loan.custom().build();
        final Investment i = Investment.fresh(l, 1000);
        assertThat(i.getRemainingPrincipal()).isEqualTo(BigDecimal.valueOf(1000));
    }

    @Test
    @DisplayName("Fresh from MarketplaceLoan works.")
    void freshFromMarketplaceLoan() {
        final MarketplaceLoan l = MarketplaceLoan.custom().build();
        final Investment i = Investment.fresh(l, 1000);
        assertThat(i.getRemainingPrincipal()).isEqualTo(BigDecimal.valueOf(1000));
    }

    @Test
    @DisplayName("Fresh from Participation works.")
    void freshFromParticipation() {
        final Loan l = Loan.custom().build();
        final Participation p = mock(Participation.class);
        when(p.getInvestmentId()).thenReturn(1L);
        when(p.getRemainingInstalmentCount()).thenReturn(2);
        final Investment i = Investment.fresh(p, l, BigDecimal.valueOf(1000));
        assertSoftly(softly -> {
            softly.assertThat(i.getRemainingPrincipal()).isEqualTo(BigDecimal.valueOf(1000));
            softly.assertThat(i.getId()).isEqualTo(p.getInvestmentId());
            softly.assertThat(i.getRemainingMonths()).isEqualTo(p.getRemainingInstalmentCount());
            softly.assertThat(i.getInvestmentDate()).isBeforeOrEqualTo(OffsetDateTime.now());
        });
    }

    @Nested
    @DisplayName("Setters for ")
    class SetterTest {

        private final InvestmentBuilder b = Investment.custom();

        private <T> void standard(final InvestmentBuilder builder, final Function<T, InvestmentBuilder> setter,
                                  final Supplier<T> getter, final T value) {
            assertThat(getter.get()).as("Null before setting.").isNull();
            final InvestmentBuilder newBuilder = setter.apply(value);
            assertSoftly(softly -> {
                softly.assertThat(newBuilder).as("Setter returned itself.").isSameAs(builder);
                softly.assertThat(getter.get()).as("Correct value was set.").isEqualTo(value);
            });
        }

        private void bool(final InvestmentBuilder builder, final Function<Boolean, InvestmentBuilder> setter,
                          final Supplier<Boolean> getter) {
            assertThat(getter.get()).as("False before setting.").isFalse();
            final InvestmentBuilder newBuilder = setter.apply(true);
            assertSoftly(softly -> {
                softly.assertThat(newBuilder).as("Setter returned itself.").isSameAs(builder);
                softly.assertThat(getter.get()).as("Correct value was set.").isTrue();
            });
        }

        private void integer(final InvestmentBuilder builder, final Function<Integer, InvestmentBuilder> setter,
                             final Supplier<Integer> getter, final int value) {
            assertThat(getter.get()).as("False before setting.").isLessThanOrEqualTo(0);
            final InvestmentBuilder newBuilder = setter.apply(value);
            assertSoftly(softly -> {
                softly.assertThat(newBuilder).as("Setter returned itself.").isSameAs(builder);
                softly.assertThat(getter.get()).as("Correct value was set.").isEqualTo(value);
            });
        }

        private <T> void optional(final InvestmentBuilder builder, final Function<T, InvestmentBuilder> setter,
                                  final Supplier<Optional<T>> getter, final T value) {
            assertThat(getter.get()).isEmpty();
            final InvestmentBuilder newBuilder = setter.apply(value);
            assertSoftly(softly -> {
                softly.assertThat(newBuilder).as("Setter returned itself.").isSameAs(builder);
                softly.assertThat(getter.get()).as("Correct value was set.").contains(value);
            });
        }

        @Test
        void loanId() {
            integer(b, b::setLoanId, b::getLoanId, 1);
        }

        @Test
        void daysPastDue() {
            integer(b, b::setDaysPastDue, b::getDaysPastDue, 1);
        }

        @Test
        void originalTerm() {
            integer(b, b::setOriginalTerm, b::getOriginalTerm, 1);
        }

        @Test
        void currentTerm() {
            integer(b, b::setCurrentTerm, b::getCurrentTerm, 1);
        }

        @Test
        void remainingMonths() {
            integer(b, b::setRemainingMonths, b::getRemainingMonths, 1);
        }

        @Test
        void onSmp() {
            bool(b, b::setOnSmp, b::isOnSmp);
        }

        @Test
        void canBeOffered() {
            bool(b, b::setOfferable, b::canBeOffered);
        }

        @Test
        void insuranceActive() {
            bool(b, b::setInsuranceActive, b::isInsuranceActive);
        }

        @Test
        void instalmentsPostponed() {
            bool(b, b::setInstalmentsPostponed, b::areInstalmentsPostponed);
        }

        @Test
        void inWithdrawal() {
            optional(b, b::setInWithdrawal, b::isInWithdrawal, true);
        }

        @Test
        void paymentStatus() {
            optional(b, b::setPaymentStatus, b::getPaymentStatus, PaymentStatus.COVERED);
        }

        @Test
        void smpFee() {
            optional(b, b::setSmpFee, b::getSmpFee, BigDecimal.ONE);
        }

        @Test
        void smpSoldFor() {
            optional(b, b::setSmpSoldFor, b::getSmpSoldFor, BigDecimal.ONE);
        }

        @Test
        void nextPaymentDate() {
            optional(b, b::setNextPaymentDate, b::getNextPaymentDate, OffsetDateTime.now());
        }

        @Test
        void investmentStatus() {
            standard(b, b::setStatus, b::getStatus, InvestmentStatus.ACTIVE);
        }

        @Test
        void rating() {
            standard(b, b::setRating, b::getRating, Rating.D);
        }

        @Test
        void originalPrincipal() {
            standard(b, b::setOriginalPrincipal, b::getOriginalPrincipal, BigDecimal.ONE);
        }

        @Test
        void paidPrincipal() {
            standard(b, b::setPaidPrincipal, b::getPaidPrincipal, BigDecimal.ONE);
        }

        @Test
        void duePrincipal() {
            standard(b, b::setDuePrincipal, b::getDuePrincipal, BigDecimal.ONE);
        }

        @Test
        void expectedInterest() {
            standard(b, b::setExpectedInterest, b::getExpectedInterest, BigDecimal.ONE);
        }

        @Test
        void paidInterest() {
            standard(b, b::setPaidInterest, b::getPaidInterest, BigDecimal.ONE);
        }

        @Test
        void dueInterest() {
            standard(b, b::setDueInterest, b::getDueInterest, BigDecimal.ONE);
        }

        @Test
        void paidPenalty() {
            standard(b, b::setPaidPenalty, b::getPaidPenalty, BigDecimal.ONE);
        }

        @Test
        void interestRate() {
            standard(b, b::setInterestRate, b::getInterestRate, BigDecimal.ONE);
        }

        @Test
        void revenueRate() {
            standard(b, b::setRevenueRate, b::getRevenueRate, BigDecimal.ONE);
        }
    }
}

