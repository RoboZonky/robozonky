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

package com.github.triceo.robozonky.app.investing.delinquency;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.github.triceo.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.app.investing.AbstractInvestingTest;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class DelinquencyUpdateTest extends AbstractInvestingTest {

    @Test
    public void difference() {
        final Loan l = new Loan(1, 200);
        final Loan l2 = new Loan(2, 400);
        final Collection<Loan> one = Arrays.asList(l, l2);
        final Collection<Loan> two = Collections.singletonList(l);
        Assertions.assertThat(DelinquencyUpdate.getDifference(one, two)).containsExactly(l2);
        Assertions.assertThat(DelinquencyUpdate.getDifference(two, one)).isEmpty();
    }

    @Test
    public void event() {
        final Delinquent d = new Delinquent(1, OffsetDateTime.now());
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.when(z.getLoan(ArgumentMatchers.eq(d.getLoanId()))).thenReturn(new Loan(d.getLoanId(), 200));
        DelinquencyUpdate.sendEvents(Collections.singleton(d), z, (l, i) -> new LoanDelinquentEvent(l, i.getSince()));
        Assertions.assertThat(this.getNewEvents()).first().isInstanceOf(LoanDelinquentEvent.class);
    }
}
