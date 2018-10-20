/*
 * Copyright 2018 The RoboZonky Project
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.github.robozonky.strategy.natural.Wrapper;

/**
 * Implements a complex marketplace filter. This filter accepts an item from the marketplace and, by returning true,
 * it tells the investment algorithm to not process it any further.
 * <p>
 * This is the structure of a filter:
 * <ul>
 * <li>TRUE IF CONDITIONS1 TRUE BUT NOT WHEN CONDITIONS2 TRUE.</li>
 * <li>CONDITIONS1 is TRUE when all conditions evaluate to true.</li>
 * <li>CONDITIONS2 is TRUE when either empty or at least one condition evalutes to false.</li>
 * <li>CONDITIONS1 supplied by {@link #when(Collection)}, CONDITIONS2 by {@link #butNotWhen(Collection)}.</li>
 * </ul>
 * <p>
 * Result of the evaluation obtained via {@link #test(Wrapper)}.
 */
public class MarketplaceFilter extends MarketplaceFilterConditionImpl {

    private static AtomicInteger COUNTER = new AtomicInteger(0);
    private final int id = COUNTER.incrementAndGet();
    private Collection<MarketplaceFilterCondition> when = Collections.emptySet(),
            butNotWhen = Collections.emptySet();

    public static MarketplaceFilter of(final MarketplaceFilterCondition c) {
        final MarketplaceFilter f = new MarketplaceFilter();
        f.when(Collections.singleton(c));
        return f;
    }

    private static String toString(final Collection<MarketplaceFilterCondition> conditions) {
        return conditions.stream()
                .map(MarketplaceFilterCondition::toString)
                .collect(Collectors.joining(" and "));
    }

    /**
     * {@link #test(Wrapper)} will only return true if all of the conditions supplied here return true.
     * @param conditions All must return true for filter to return true.
     */
    public void when(final Collection<? extends MarketplaceFilterCondition> conditions) {
        when = new LinkedHashSet<>(conditions);
    }

    /**
     * {@link #test(Wrapper)} will only return true if {@link #when(Collection)} and none of the conditions
     * supplied here return true.
     * @param conditions All must return false for filter to return true.
     */
    public void butNotWhen(final Collection<? extends MarketplaceFilterCondition> conditions) {
        butNotWhen = new LinkedHashSet<>(conditions);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("#" + id + ": [" + toString(when) + "] but not [" + toString(butNotWhen) + "].");
    }

    /**
     * Whether or not the item matches the complex condition.
     * @param item Item in question.
     * @return True when all {@link #when} true AND 1+ {@link #butNotWhen} false.
     */
    @Override
    public boolean test(final Wrapper<?> item) {
        final Predicate<MarketplaceFilterCondition> f = c -> c.test(item);
        return when.stream().allMatch(f) && (butNotWhen.isEmpty() || !butNotWhen.stream().allMatch(f));
    }
}
