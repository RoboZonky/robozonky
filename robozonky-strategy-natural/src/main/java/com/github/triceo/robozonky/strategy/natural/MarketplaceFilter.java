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
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.remote.entities.Loan;

public class MarketplaceFilter extends MarketplaceFilterCondition {

    private static String toString(final Collection<MarketplaceFilterCondition> conditions) {
        return conditions.stream()
                .map(MarketplaceFilterCondition::toString)
                .collect(Collectors.joining(" and "));
    }

    private Collection<MarketplaceFilterCondition> ignoreWhen = Collections.emptySet(),
        butNotWhen = Collections.emptySet();

    public void ignoreWhen(final Collection<MarketplaceFilterCondition> conditions) {
        ignoreWhen = new LinkedHashSet<>(conditions);
    }

    public void butNotWhen(final Collection<MarketplaceFilterCondition> conditions) {
        butNotWhen = new LinkedHashSet<>(conditions);
    }

    @Override
    protected Optional<String> getDescription() {
        return Optional.of("When [" + toString(ignoreWhen) + "] but not when [" + toString(butNotWhen) + "].");
    }

    /**
     * Whether or not the loan should be filtered out.
     *
     * @param loan Loan in question.
     * @return True when all the initial conditions return true AND when one or more secondary conditions don't.
     */
    @Override
    public boolean test(final Loan loan) {
        final Predicate<MarketplaceFilterCondition> f = c -> c.test(loan);
        return ignoreWhen.stream().allMatch(f) && (butNotWhen.isEmpty() || !butNotWhen.stream().allMatch(f));
    }

}
