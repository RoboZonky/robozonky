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

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class MarketplaceFilterTest {

    private static final Supplier<MarketplaceFilterCondition> MATCHING = () -> new MarketplaceFilterCondition() {

        @Override
        public boolean test(final Loan loan) {
            return true;
        }
    };
    private static final Supplier<MarketplaceFilterCondition> NOT_MATCHING = () -> new MarketplaceFilterCondition() {
        // this is the default
    };

    @Test
    public void noConditions() {
        final MarketplaceFilterCondition f = new MarketplaceFilter();
        Assertions.assertThat(f.test(Mockito.mock(Loan.class))).isTrue();
    }

    @Test
    public void oneMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.ignoreWhen(Collections.singletonList(MarketplaceFilterTest.MATCHING.get()));
        Assertions.assertThat(f.test(Mockito.mock(Loan.class))).isTrue();
    }

    @Test
    public void notAllMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.ignoreWhen(Arrays.asList(MarketplaceFilterTest.MATCHING.get(), MarketplaceFilterTest.NOT_MATCHING.get(),
                                   MarketplaceFilterTest.MATCHING.get()));
        Assertions.assertThat(f.test(Mockito.mock(Loan.class))).isFalse();
    }

    @Test
    public void secondaryOneNotMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.butNotWhen(Collections.singleton(MarketplaceFilterTest.NOT_MATCHING.get()));
        Assertions.assertThat(f.test(Mockito.mock(Loan.class))).isTrue();
    }

    @Test
    public void secondaryAllMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.butNotWhen(Arrays.asList(MarketplaceFilterTest.MATCHING.get(), MarketplaceFilterTest.MATCHING.get()));
        Assertions.assertThat(f.test(Mockito.mock(Loan.class))).isFalse();
    }
}
