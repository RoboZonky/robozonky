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
import java.util.function.Supplier;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Participation;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class MarketplaceFilterTest {

    private static final Supplier<PrimaryMarketplaceFilterCondition> PRIMARY_MATCHING = () -> new
            PrimaryMarketplaceFilterCondition() {

        @Override
        public boolean test(final Loan loan) {
            return true;
        }
    };
    private static final Supplier<PrimaryMarketplaceFilterCondition> PRIMARY_NOT_MATCHING = () -> new
            PrimaryMarketplaceFilterCondition() {
        // this is the default
    };
    private static final Supplier<SecondaryMarketplaceFilterCondition> SECONDARY_MATCHING = () -> new
            SecondaryMarketplaceFilterCondition() {

        @Override
        public boolean test(final Participation loan) {
            return true;
        }
    };
    private static final Supplier<SecondaryMarketplaceFilterCondition> SECONDARY_NOT_MATCHING = () -> new
            SecondaryMarketplaceFilterCondition() {
        // this is the default
    };
    private static final Supplier<JointMarketplaceFilterCondition> JOINT_MATCHING = () -> new
            JointMarketplaceFilterCondition() {

        @Override
        public boolean test(final Wrapper loan) {
            return true;
        }
    };
    private static final Supplier<JointMarketplaceFilterCondition> JOINT_NOT_MATCHING = () -> new
            JointMarketplaceFilterCondition() {
        // this is the default
    };

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        final Collection<Object[]> result = new ArrayList<>();
        result.add(new Object[]{Mockito.mock(Loan.class), PRIMARY_MATCHING, PRIMARY_NOT_MATCHING});
        result.add(new Object[]{Mockito.mock(Participation.class), SECONDARY_MATCHING, SECONDARY_NOT_MATCHING});
        result.add(new Object[]{Mockito.mock(Loan.class), JOINT_MATCHING, JOINT_NOT_MATCHING});
        result.add(new Object[]{Mockito.mock(Participation.class), JOINT_MATCHING, JOINT_NOT_MATCHING});
        return result;
    }

    @Parameterized.Parameter
    public Object toMatch;
    @Parameterized.Parameter(1)
    public Supplier<MarketplaceFilterCondition> matching;
    @Parameterized.Parameter(2)
    public Supplier<MarketplaceFilterCondition> notMatching;

    @Test
    public void noConditions() {
        final MarketplaceFilterConditionImpl f = new MarketplaceFilter();
        Assertions.assertThat(f.test(toMatch)).isTrue();
    }

    @Test
    public void oneMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.ignoreWhen(Collections.singletonList(matching.get()));
        Assertions.assertThat(f.test(toMatch)).isTrue();
    }

    @Test
    public void notAllMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.ignoreWhen(Arrays.asList(matching.get(), notMatching.get(), matching.get()));
        Assertions.assertThat(f.test(toMatch)).isFalse();
    }

    @Test
    public void secondaryOneNotMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.butNotWhen(Collections.singleton(notMatching.get()));
        Assertions.assertThat(f.test(toMatch)).isTrue();
    }

    @Test
    public void secondaryAllMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.butNotWhen(Arrays.asList(matching.get(), matching.get()));
        Assertions.assertThat(f.test(toMatch)).isFalse();
    }
}
