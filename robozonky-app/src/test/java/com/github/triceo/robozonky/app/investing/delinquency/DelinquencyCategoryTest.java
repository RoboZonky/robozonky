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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.common.AbstractStateLeveragingTest;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DelinquencyCategoryTest extends AbstractStateLeveragingTest {

    private static final Delinquent UNDER_10 = new Delinquent(1, OffsetDateTime.now()),
            UNDER_30 = new Delinquent(3, OffsetDateTime.now().minusDays(10)),
            UNDER_60 = new Delinquent(4, OffsetDateTime.now().minusDays(30)),
            UNDER_90 = new Delinquent(5, OffsetDateTime.now().minusDays(60)),
            OVER_90 = new Delinquent(6, OffsetDateTime.now().minusDays(90));

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        final Collection<Object[]> result = new ArrayList<>(DelinquencyCategory.values().length);
        result.add(new Object[]{DelinquencyCategory.MILD, UNDER_10, UNDER_30});
        result.add(new Object[]{DelinquencyCategory.SEVERE, UNDER_30, UNDER_60});
        result.add(new Object[]{DelinquencyCategory.CRITICAL, UNDER_60, UNDER_90});
        result.add(new Object[]{DelinquencyCategory.DEFAULTED, UNDER_90, OVER_90});
        return result;
    }

    @Parameterized.Parameter
    public DelinquencyCategory known;
    @Parameterized.Parameter(1)
    public Delinquent under;
    @Parameterized.Parameter(2)
    public Delinquent over;

    @Test
    public void updateAndPurge() {
        Assertions.assertThat(known.getKnownDelinquents()).isEmpty();
        known.updateKnownDelinquents(Arrays.asList(under, over));
        Assertions.assertThat(known.getKnownDelinquents()).containsExactly(over);
        final Loan l = new Loan(over.getLoanId(), 200);
        final Investment i = new Investment(l, (int) l.getAmount());
        known.purge(Collections.singleton(i));
        Assertions.assertThat(known.getKnownDelinquents()).isEmpty();
    }
}
