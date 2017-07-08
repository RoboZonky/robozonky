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

package com.github.triceo.robozonky.strategy.natural;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class EqualityConditionTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        final List<Rating> all = Arrays.asList(Rating.values());
        final int total = all.size();
        final Collection<Object[]> result = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            final Rating current = all.get(i);
            final Collection<Rating> betterThan = i == 0 ? Collections.emptyList() : all.subList(0, i);
            final Collection<Rating> worseThan = i == total - 1 ? Collections.emptyList() : all.subList(i + 1, total);
            result.add(new Object[] {current, betterThan, worseThan});
        }
        return result;
    }

    @Parameterized.Parameter
    public Rating current;
    @Parameterized.Parameter(1)
    public Collection<Rating> betterThanCurrent;
    @Parameterized.Parameter(2)
    public Collection<Rating> worseThanCurrent;

    private Loan mockLoan(final Rating r) {
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getRating()).thenReturn(r);
        return l;
    }

    @Test
    public void testBetterThan() {
        System.out.println(current + " " + betterThanCurrent + " " + worseThanCurrent);
        final MarketplaceFilterCondition c = new LoanRatingBetterOrEqualCondition(current);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(c.test(mockLoan(current))).isTrue();
            betterThanCurrent.forEach(r -> softly.assertThat(c.test(mockLoan(r))).isTrue());
            worseThanCurrent.forEach(r -> softly.assertThat(c.test(mockLoan(r))).isFalse());
        });
    }

    @Test
    public void testWorseThan() {
        final MarketplaceFilterCondition c = new LoanRatingWorseOrEqualCondition(current);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(c.test(mockLoan(current))).isTrue();
            betterThanCurrent.forEach(r -> softly.assertThat(c.test(mockLoan(r))).isFalse());
            worseThanCurrent.forEach(r -> softly.assertThat(c.test(mockLoan(r))).isTrue());
        });
    }

}
