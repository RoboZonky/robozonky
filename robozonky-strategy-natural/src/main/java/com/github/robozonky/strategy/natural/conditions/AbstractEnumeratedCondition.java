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

package com.github.robozonky.strategy.natural.conditions;

import java.util.Collection;
import java.util.Optional;

import com.github.robozonky.strategy.natural.InvestmentWrapper;
import com.github.robozonky.strategy.natural.LoanWrapper;
import com.github.robozonky.strategy.natural.ParticipationWrapper;
import com.github.robozonky.strategy.natural.Wrapper;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

class AbstractEnumeratedCondition<T> extends MarketplaceFilterConditionImpl implements MarketplaceFilterCondition {

    private final Collection<T> possibleValues = new UnifiedSet<>(0);

    public void add(final T item) {
        this.possibleValues.add(item);
    }

    public void add(final Collection<T> items) {
        items.forEach(this::add);
    }

    protected T retrieve(final Wrapper wrapper) {
        throw new UnsupportedOperationException();
    }

    protected T retrieve(final InvestmentWrapper wrapper) {
        return retrieve((Wrapper) wrapper);
    }

    protected T retrieve(final ParticipationWrapper wrapper) {
        return retrieve((Wrapper) wrapper);
    }

    protected T retrieve(final LoanWrapper wrapper) {
        return retrieve((Wrapper) wrapper);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Possible values: " + possibleValues + '.');
    }

    @Override
    public boolean test(final LoanWrapper item) {
        return new EnumeratedCondition<LoanWrapper, T>(this::retrieve, possibleValues).test(item);
    }

    @Override
    public boolean test(final ParticipationWrapper item) {
        return new EnumeratedCondition<ParticipationWrapper, T>(this::retrieve, possibleValues).test(item);
    }

    @Override
    public boolean test(final InvestmentWrapper item) {
        return new EnumeratedCondition<InvestmentWrapper, T>(this::retrieve, possibleValues).test(item);
    }
}
