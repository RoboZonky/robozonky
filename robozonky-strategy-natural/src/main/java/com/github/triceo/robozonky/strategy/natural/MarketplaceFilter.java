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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.function.Predicate;

import com.github.triceo.robozonky.api.remote.entities.Loan;

class MarketplaceFilter extends MarketplaceFilterCondition {

    private Collection<MarketplaceFilterCondition> when = Collections.emptySet(),
        butNotWhen = Collections.emptySet();

    public void when(final Collection<MarketplaceFilterCondition> conditions) {
        when = new LinkedHashSet<>(conditions);
    }

    public void butNotWhen(final Collection<MarketplaceFilterCondition> conditions) {
        butNotWhen = new LinkedHashSet<>(conditions);
    }

    @Override
    public boolean test(final Loan loan) {
        final Predicate<MarketplaceFilterCondition> f = c -> c.test(loan);
        return when.stream().allMatch(f) && butNotWhen.stream().noneMatch(f);
    }

}
