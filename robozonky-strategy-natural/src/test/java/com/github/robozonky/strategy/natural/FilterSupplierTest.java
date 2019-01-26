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

package com.github.robozonky.strategy.natural;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class FilterSupplierTest {

    @Test
    void extremeExitDate() { // https://github.com/RoboZonky/robozonky/issues/219
        final DefaultValues v = new DefaultValues(DefaultPortfolio.PROGRESSIVE);
        v.setExitProperties(new ExitProperties(LocalDate.MAX));
        final FilterSupplier s = new FilterSupplier(v, Collections.emptySet(), Collections.emptySet(),
                                                    Collections.emptySet());
        assertSoftly(softly -> {
            softly.assertThat(s.getPrimaryMarketplaceFilters()).isNotEmpty();
            softly.assertThat(s.getSecondaryMarketplaceFilters()).isNotEmpty();
            softly.assertThat(s.getSellFilters()).isEmpty();
        });
    }

    @Test
    void nullHandling() {
        final FilterSupplier f = new FilterSupplier(new DefaultValues(DefaultPortfolio.PROGRESSIVE), null, null,
                                                    Collections.emptyList());
        assertSoftly(softly -> {
            softly.assertThat(f.getPrimaryMarketplaceFilters()).isEmpty();
            softly.assertThat(f.getSecondaryMarketplaceFilters()).isEmpty();
            softly.assertThat(f.getSellFilters()).isEmpty();
        });
    }
}
