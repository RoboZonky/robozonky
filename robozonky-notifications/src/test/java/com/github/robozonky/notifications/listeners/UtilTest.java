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

package com.github.robozonky.notifications.listeners;

import static org.assertj.core.api.Assertions.*;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.notifications.LoanBased;
import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.test.mock.MockLoanBuilder;

class UtilTest {

    @Test
    void emailObfuscation() {
        assertThat(Util.obfuscateEmailAddress("someone@somewhere.net")).isEqualTo("s...e@s...t");
        assertThat(Util.obfuscateEmailAddress("ab@cd")).isEqualTo("a...b@c...d");
        // too short to obfuscate
        assertThat(Util.obfuscateEmailAddress("a@b")).isEqualTo("a@b");
    }

    @Test
    void stackTrace() {
        final String result = Util.stackTraceToString(new IllegalStateException());
        assertThat(result).contains("IllegalStateException");
    }

    @Test
    void isSocketTimeout() {
        final Throwable t = new SocketTimeoutException("Testing");
        assertThat(Util.isNetworkProblem(t)).isTrue();
        final Throwable t2 = new IllegalStateException(t);
        assertThat(Util.isNetworkProblem(t2)).isTrue();
        final Throwable t3 = new IllegalStateException();
        assertThat(Util.isNetworkProblem(t3)).isFalse();
        assertThat(Util.isNetworkProblem(null)).isFalse();
    }

    @Test
    void isSocket() {
        final Throwable t = new SocketException("Testing");
        assertThat(Util.isNetworkProblem(t)).isTrue();
        final Throwable t2 = new IllegalStateException(t);
        assertThat(Util.isNetworkProblem(t2)).isTrue();
        final Throwable t3 = new IllegalStateException();
        assertThat(Util.isNetworkProblem(t3)).isFalse();
        assertThat(Util.isNetworkProblem(null)).isFalse();
    }

    @Test
    void identifyLoanBased() {
        final LoanBased l = new LoanDefaultedEvent() {

            @Override
            public OffsetDateTime getCreatedOn() {
                return OffsetDateTime.now();
            }

            @Override
            public Loan getLoan() {
                return new MockLoanBuilder()
                    .set(LoanImpl::setRating, Rating.D)
                    .set(LoanImpl::setInterestRate, Ratio.ONE)
                    .build();
            }

            @Override
            public Investment getInvestment() {
                return null;
            }

            @Override
            public LocalDate getDelinquentSince() {
                return LocalDate.now();
            }

        };
        assertThat(Util.identifyLoan(l)).isNotEmpty();
    }
}
