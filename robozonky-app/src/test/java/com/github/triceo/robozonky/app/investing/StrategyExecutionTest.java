/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.investing;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class StrategyExecutionTest extends AbstractInvestingTest {

    @Test
    public void noStrategy() {
        final Loan loan = new Loan(1, 2);
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Refreshable<InvestmentStrategy> r = Refreshable.createImmutable(null);
        r.run();
        final StrategyExecution exec = new StrategyExecution(null, r, null, Duration.ofMinutes(60));
        Assertions.assertThat(exec.apply(Collections.singletonList(ld))).isEmpty();
        // check events
        final List<Event> events = this.getNewEvents();
        Assertions.assertThat(events).isEmpty();
    }
}
