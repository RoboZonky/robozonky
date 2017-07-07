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
import java.util.LinkedHashSet;

import com.github.triceo.robozonky.api.remote.entities.Loan;

class MarketplaceFilterConditions extends MarketplaceFilterCondition {

    private final Collection<MarketplaceFilterCondition> conditions = new LinkedHashSet<>(0);

    public void add(final MarketplaceFilterCondition condition) {
        conditions.add(condition);
    }

    @Override
    public boolean test(final Loan loan) {
        return conditions.stream().allMatch(condition -> condition.test(loan));
    }

}
