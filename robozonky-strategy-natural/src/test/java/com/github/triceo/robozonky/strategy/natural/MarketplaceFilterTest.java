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

package com.github.triceo.robozonky.strategy.natural;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class MarketplaceFilterTest {

    private static final Supplier<MarketplaceFilterCondition> MATCHING = () -> new MarketplaceFilterCondition() {

        @Override
        public boolean test(final Wrapper loan) {
            return true;
        }
    };
    private static final Supplier<MarketplaceFilterCondition> NOT_MATCHING = () -> new MarketplaceFilterCondition() {
        // this is the default
    };

    @Test
    public void noConditions() {
        final MarketplaceFilterConditionImpl f = new MarketplaceFilter();
        Assertions.assertThat(f.test(Mockito.mock(Wrapper.class))).isTrue();
    }

    @Test
    public void oneMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.ignoreWhen(Collections.singletonList(MATCHING.get()));
        Assertions.assertThat(f.test(Mockito.mock(Wrapper.class))).isTrue();
    }

    @Test
    public void notAllMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.ignoreWhen(Arrays.asList(MATCHING.get(), NOT_MATCHING.get(), MATCHING.get()));
        Assertions.assertThat(f.test(Mockito.mock(Wrapper.class))).isFalse();
    }

    @Test
    public void secondaryOneNotMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.butNotWhen(Collections.singleton(NOT_MATCHING.get()));
        Assertions.assertThat(f.test(Mockito.mock(Wrapper.class))).isTrue();
    }

    @Test
    public void secondaryAllMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.butNotWhen(Arrays.asList(MATCHING.get(), MATCHING.get()));
        Assertions.assertThat(f.test(Mockito.mock(Wrapper.class))).isFalse();
    }
}
