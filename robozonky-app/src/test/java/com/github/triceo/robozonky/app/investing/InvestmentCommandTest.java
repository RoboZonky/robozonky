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

import java.util.Collection;
import java.util.Collections;

import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class InvestmentCommandTest extends AbstractInvestingTest {

    @Test
    public void empty() {
        final Collection<LoanDescriptor> loans = Collections.emptyList();
        final Session sess = Mockito.mock(Session.class);
        final InvestmentStrategy s = Mockito.mock(InvestmentStrategy.class);
        final InvestmentCommand c = new StrategyBasedInvestmentCommand(s, loans);
        c.accept(sess); // SUT
        Assertions.assertThat(sess.getInvestmentsMade()).isEmpty();
        Mockito.verify(sess, Mockito.never()).invest(ArgumentMatchers.any());
    }
}
