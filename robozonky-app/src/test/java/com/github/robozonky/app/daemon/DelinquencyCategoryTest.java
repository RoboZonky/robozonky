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

import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DelinquencyCategoryTest extends AbstractZonkyLeveragingTest {

    private static void testEmpty(final DelinquencyCategory category) {
        final TransactionalPortfolio portfolio = new TransactionalPortfolio(null, mockTenant());
        assertThat(category.update(portfolio, Collections.emptyList())).isEmpty();
    }

    private void reinit() { // JUnit 5 doesn't execute before/after methods for dynamic tests
        this.deleteState();
        this.readPreexistingEvents();
    }

    private void testAddAndRead(final DelinquencyCategory category, final Period minimumMatchingDuration) {
        this.reinit();
        final int loanId = 1;
        final Loan loan = Loan.custom().setId(loanId).setAmount(200).build();
        // store a delinquent loan
        final Investment i = Investment.fresh(loan, 200)
                .setDaysPastDue(minimumMatchingDuration.getDays())
                .build();
        final Zonky z = harmlessZonky(10_000);
        when(z.getLoan(eq(loanId))).thenReturn(loan);
        final Tenant t = mockTenant(z);
        final TransactionalPortfolio portfolio = new TransactionalPortfolio(null, t);
        assertThat(category.update(portfolio, Collections.singleton(i)))
                .containsExactly(loanId);
        portfolio.run(); // finish the transaction
        final List<Event> events = getEventsRequested();
        assertSoftly(softly -> {
            softly.assertThat(events).hasSize(1);
            softly.assertThat(events).first().isInstanceOf(LoanDelinquentEvent.class);
        });
        // attempt to store it again, making sure no event is fired
        assertThat(category.update(portfolio, Collections.singleton(i)))
                .containsExactly(loanId);
        assertThat(getEventsRequested()).hasSize(1);
        // now update with no delinquents, making sure nothing is returned
        assertThat(category.update(portfolio, Collections.emptyList()))
                .isEmpty();
        assertThat(getEventsRequested()).hasSize(1);
    }

    @TestFactory
    Stream<DynamicNode> categories() {
        return Stream.of(DelinquencyCategory.values())
                .filter(c -> c != DelinquencyCategory.DEFAULTED)
                .map(category -> {
                    final Period minimumMatchingDuration = Period.ofDays(category.getThresholdInDays());
                    return dynamicContainer(category.toString(), Stream.of(
                            dynamicTest("updates", () -> testAddAndRead(category, minimumMatchingDuration)),
                            dynamicTest("empty", () -> testEmpty(category))
                    ));
                });
    }
}
