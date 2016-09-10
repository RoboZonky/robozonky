/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.ZotifyApi;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class MarketplaceTest {

    private static Loan mockLoan(final double amount) {
        final Loan mock = Mockito.mock(Loan.class);
        Mockito.when(mock.getRemainingInvestment()).thenReturn(amount);
        return mock;
    }

    private static Loan mockLoan(final Instant publishingDate) {
        final Loan mock = Mockito.mock(Loan.class);
        Mockito.when(mock.getDatePublished()).thenReturn(publishingDate);
        Mockito.when(mock.getRemainingInvestment()).thenReturn(1000.0);
        return mock;
    }

    @Test
    public void standardQuerying() {
        // setup
        final Loan nothingToInvest = MarketplaceTest.mockLoan(0.0); // will be excluded
        final Loan veryOldLoan = MarketplaceTest.mockLoan(Instant.MIN);
        final Loan veryRecentLoan = MarketplaceTest.mockLoan(Instant.now().minus(1, ChronoUnit.MINUTES));
        final Loan futureLoan = MarketplaceTest.mockLoan(Instant.now().plus(1, ChronoUnit.HOURS));
        final ZotifyApi apiMock = Mockito.mock(ZotifyApi.class);
        Mockito.when(apiMock.getLoans())
                .thenReturn(Arrays.asList(veryOldLoan, nothingToInvest, futureLoan, veryRecentLoan));
        // test
        final Marketplace m = Marketplace.from(apiMock);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(m.getAllLoans()).containsExactly(futureLoan, veryRecentLoan, veryOldLoan);
        softly.assertThat(m.getLoansOlderThan(30)).containsExactly(veryRecentLoan, veryOldLoan);
        softly.assertThat(m.getLoansNewerThan(Instant.now())).containsExactly(futureLoan);
        softly.assertAll();
    }

}
