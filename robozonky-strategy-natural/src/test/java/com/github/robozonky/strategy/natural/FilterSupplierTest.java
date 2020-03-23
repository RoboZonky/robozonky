/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.strategy.natural;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.robozonky.strategy.natural.conditions.LoanTermCondition;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilterCondition;
import com.github.robozonky.strategy.natural.conditions.VeryShortStoryCondition;

class FilterSupplierTest {

    @Test
    void filterOrdering() { // Fast filters come first as they do not require HTTP requests to be made.
        MarketplaceFilterCondition requiresRequests = new VeryShortStoryCondition();
        MarketplaceFilterCondition doesNotRequireRequests = LoanTermCondition.lessThan(10);
        MarketplaceFilter slowFilter = new MarketplaceFilter();
        slowFilter.when(List.of(requiresRequests));
        MarketplaceFilter fastFilter = new MarketplaceFilter();
        fastFilter.when(List.of(doesNotRequireRequests));
        FilterSupplier filters = new FilterSupplier(new DefaultValues(DefaultPortfolio.PROGRESSIVE),
                Arrays.asList(slowFilter, fastFilter),
                Arrays.asList(slowFilter, fastFilter),
                Collections.emptyList());
        assertSoftly(softly -> {
            softly.assertThat(filters.getPrimaryMarketplaceFilters())
                .containsExactly(fastFilter, slowFilter);
            softly.assertThat(filters.getSecondaryMarketplaceFilters())
                .containsExactly(fastFilter, slowFilter);
        });
    }

    @Test
    void extremeExitDate() { // https://github.com/RoboZonky/robozonky/issues/219
        final DefaultValues v = new DefaultValues(DefaultPortfolio.PROGRESSIVE);
        v.setExitProperties(new ExitProperties(LocalDate.MAX));
        final FilterSupplier s = new FilterSupplier(v, Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet());
        assertSoftly(softly -> {
            softly.assertThat(s.getPrimaryMarketplaceFilters())
                .isNotEmpty();
            softly.assertThat(s.getSecondaryMarketplaceFilters())
                .isNotEmpty();
            softly.assertThat(s.getSellFilters())
                .isEmpty();
        });
    }

    @Test
    void nullHandling() {
        final FilterSupplier f = new FilterSupplier(new DefaultValues(DefaultPortfolio.PROGRESSIVE), null, null,
                Collections.emptyList());
        assertSoftly(softly -> {
            softly.assertThat(f.getPrimaryMarketplaceFilters())
                .isEmpty();
            softly.assertThat(f.getSecondaryMarketplaceFilters())
                .isEmpty();
            softly.assertThat(f.getSellFilters())
                .isEmpty();
        });
    }
}
