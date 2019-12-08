/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.strategy.natural.conditions;

import java.util.Arrays;
import java.util.Collections;

import com.github.robozonky.strategy.natural.Wrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarketplaceFilterTest {

    private static final MarketplaceFilterCondition MATCHING = new MarketplaceFilterConditionImpl() {
        @Override
        public boolean test(Wrapper<?> item) {
            return true;
        }
    };
    private static final MarketplaceFilterCondition NOT_MATCHING = MATCHING.negate();

    @Test
    void noConditions() {
        final MarketplaceFilterCondition f = new MarketplaceFilter();
        assertThat(f.test(mock(Wrapper.class))).isTrue();
        assertThat(f.toString()).isNotEmpty();
    }

    @Test
    void oneMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.when(Collections.singletonList(MATCHING));
        assertThat(f.test(mock(Wrapper.class))).isTrue();
    }

    @Test
    void notAllMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.when(Arrays.asList(MATCHING, NOT_MATCHING, MATCHING));
        assertThat(f.test(mock(Wrapper.class))).isFalse();
    }

    @Test
    void secondaryOneNotMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.butNotWhen(Collections.singleton(NOT_MATCHING));
        assertThat(f.test(mock(Wrapper.class))).isTrue();
    }

    @Test
    void secondaryAllMatching() {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.butNotWhen(Arrays.asList(MATCHING, MATCHING));
        assertThat(f.test(mock(Wrapper.class))).isFalse();
    }
}
