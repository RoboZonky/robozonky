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

package com.github.robozonky.app.portfolio;

import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.Transactional;
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

    private void testEmpty(final DelinquencyCategory category) {
        final Transactional portfolio = new Transactional(null, mockTenant());
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
                .setNextPaymentDate(LocalDate.now().minus(minimumMatchingDuration).atTime(OffsetTime.now()))
                .build();
        final Zonky z = harmlessZonky(10_000);
        when(z.getLoan(eq(loanId))).thenReturn(loan);
        final Tenant t = mockTenant(z);
        final Transactional portfolio = new Transactional(null, t);
        assertThat(category.update(portfolio, Collections.singleton(i)))
                .containsExactly(loanId);
        portfolio.run(); // finish the transaction
        final List<Event> events = this.getNewEvents();
        assertSoftly(softly -> {
            softly.assertThat(events).hasSize(1);
            softly.assertThat(events).first().isInstanceOf(LoanDelinquentEvent.class);
        });
        // attempt to store it again, making sure no event is fired
        assertThat(category.update(portfolio, Collections.singleton(i)))
                .containsExactly(loanId);
        assertThat(this.getNewEvents()).hasSize(1);
        // now update with no delinquents, making sure nothing is returned
        assertThat(category.update(portfolio, Collections.emptyList()))
                .isEmpty();
        assertThat(this.getNewEvents()).hasSize(1);
    }

    @TestFactory
    Stream<DynamicNode> categories() {
        return Stream.of(DelinquencyCategory.values())
                .map(category -> {
                    final Period minimumMatchindDuration = Period.ofDays(category.getThresholdInDays());
                    return dynamicContainer(category.toString(), Stream.of(
                            dynamicTest("updates", () -> testAddAndRead(category, minimumMatchindDuration)),
                            dynamicTest("empty", () -> testEmpty(category))
                    ));
                });
    }
}
