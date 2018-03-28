/*
 * Copyright 2017 The RoboZonky Project
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
import java.time.Period;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class DelinquencyCategoryTest extends AbstractZonkyLeveragingTest {

    private static final Function<Integer, Investment> INVESTMENT_SUPPLIER =
            (id) -> Investment.custom().build();
    private static final Function<Loan, Collection<Development>> COLLECTIONS_SUPPLIER =
            (l) -> Collections.emptyList();

    private static void testEmpty(final DelinquencyCategory category) {
        assertThat(category.update(Collections.emptyList(), null, null, null)).isEmpty();
    }

    private void reinit() { // JUnit 5 doesn't execute before/after methods for dynamic tests
        this.deleteState();
        this.readPreexistingEvents();
    }

    private void testAddAndRead(final DelinquencyCategory category, final Period minimumMatchingDuration) {
        this.reinit();
        final int loanId = 1;
        final Function<Investment, Loan> f = (i) -> Loan.custom().setId(loanId).setAmount(200).build();
        // store a delinquent loan
        final Delinquent d = new Delinquent(loanId);
        final Delinquency dy = d.addDelinquency(LocalDate.now().minus(minimumMatchingDuration));
        assertThat(category.update(Collections.singleton(dy), INVESTMENT_SUPPLIER, f, COLLECTIONS_SUPPLIER))
                .containsExactly(loanId);
        final List<Event> events = this.getNewEvents();
        assertSoftly(softly -> {
            softly.assertThat(events).hasSize(1);
            softly.assertThat(events).first().isInstanceOf(LoanDelinquentEvent.class);
        });
        // attempt to store it again, making sure no event is fired
        assertThat(category.update(Collections.singleton(dy), INVESTMENT_SUPPLIER, f, COLLECTIONS_SUPPLIER))
                .containsExactly(loanId);
        assertThat(this.getNewEvents()).isEqualTo(events);
        // now update with no delinquents, making sure nothing is returned
        assertThat(category.update(Collections.emptyList(), INVESTMENT_SUPPLIER, f, COLLECTIONS_SUPPLIER)).isEmpty();
        assertThat(this.getNewEvents()).isEqualTo(events);
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
