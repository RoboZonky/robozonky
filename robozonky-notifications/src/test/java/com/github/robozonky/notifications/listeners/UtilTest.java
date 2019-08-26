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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.notifications.LoanBased;
import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanRecommendedEvent;
import com.github.robozonky.api.notifications.MarketplaceLoanBased;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.remote.enums.DevelopmentType;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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
    void missingToDateInCollectionHistory() throws MalformedURLException { // https://github.com/RoboZonky/robozonky/issues/278
        final Development d = Development.custom()
                .setDateFrom(OffsetDateTime.now())
                .setType(DevelopmentType.OTHER)
                .build();
        final Loan l = Loan.custom()
                .setRating(Rating.D)
                .setAnnuity(BigDecimal.TEN)
                .setUrl(new URL("http://localhost"))
                .setRegion(Region.JIHOCESKY)
                .setMainIncomeType(MainIncomeType.EMPLOYMENT)
                .setPurpose(Purpose.AUTO_MOTO)
                .setName(UUID.randomUUID().toString())
                .build();
        final Investment i = Investment.fresh(l, 200).build();
        Util.getDelinquentData(i, l, Collections.singleton(d), LocalDate.now());
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
    void identifyMarketplaceLoanBased() {
        final MarketplaceLoanBased l = new LoanRecommendedEvent() {
            @Override
            public MarketplaceLoan getLoan() {
                return MarketplaceLoan.custom().setRating(Rating.C).setInterestRate(Ratio.ONE).build();
            }

            @Override
            public BigDecimal getRecommendation() {
                return BigDecimal.ZERO;
            }

            @Override
            public OffsetDateTime getCreatedOn() {
                return OffsetDateTime.now();
            }
        };
        assertThat(Util.identifyLoan(l)).isNotEmpty();
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
                return Loan.custom().setRating(Rating.D).setInterestRate(Ratio.ONE).build();
            }

            @Override
            public Investment getInvestment() {
                return null;
            }

            @Override
            public LocalDate getDelinquentSince() {
                return LocalDate.now();
            }

            @Override
            public Collection<Development> getCollectionActions() {
                return Collections.emptyList();
            }
        };
        assertThat(Util.identifyLoan(l)).isNotEmpty();
    }
}
