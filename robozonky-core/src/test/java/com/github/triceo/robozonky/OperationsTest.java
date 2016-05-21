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
package com.github.triceo.robozonky;

import java.util.Arrays;
import java.util.List;

import com.github.triceo.robozonky.remote.Investment;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class OperationsTest {

    private static Investment getMockInvestmentWithId(final int id) {
        final Investment i = Mockito.mock(Investment.class);
        Mockito.when(i.getLoanId()).thenReturn(id);
        return i;
    }

    @Test
    public void mergingTwoInvestmentCollectinsWorksProperly() {
        final Investment I1 = OperationsTest.getMockInvestmentWithId(1);
        final Investment I2 = OperationsTest.getMockInvestmentWithId(2);
        final Investment I3 = OperationsTest.getMockInvestmentWithId(3);

        // two identical investments will result in one
        final List<Investment> a = Arrays.asList(I1, I2);
        final List<Investment> b = Arrays.asList(I2, I3);
        Assertions.assertThat(Operations.mergeInvestments(a, b)).containsExactly(I1, I2, I3);

        // standard merging also works
        final List<Investment> c = Arrays.asList(I3);
        Assertions.assertThat(Operations.mergeInvestments(a, c)).containsExactly(I1, I2, I3);

        // reverse-order merging works
        final List<Investment> d = Arrays.asList(I2, I1);
        Assertions.assertThat(Operations.mergeInvestments(a, d)).containsExactly(I1, I2);

        // two non-identical loans with same ID are merged in the order in which they came
        final Investment I3_2 = OperationsTest.getMockInvestmentWithId(3);
        final List<Investment> e = Arrays.asList(I3_2);
        Assertions.assertThat(Operations.mergeInvestments(c, e)).containsExactly(I3);
        Assertions.assertThat(Operations.mergeInvestments(e, c)).containsExactly(I3_2);
    }

}
