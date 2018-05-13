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

import java.util.Optional;
import java.util.function.Predicate;

import com.github.robozonky.strategy.natural.InvestmentWrapper;
import com.github.robozonky.strategy.natural.LoanWrapper;
import com.github.robozonky.strategy.natural.ParticipationWrapper;
import com.github.robozonky.strategy.natural.Wrapper;

/**
 * Individual condition that may then be aggregated within {@link MarketplaceFilter}.
 */
public interface MarketplaceFilterCondition extends Predicate<Wrapper> {

    static MarketplaceFilterCondition alwaysAccepting() {
        return AlwaysAcceptingCondition.INSTANCE;
    }

    static MarketplaceFilterCondition neverAccepting() {
        return NeverAceptingCondition.INSTANCE;
    }

    /**
     * Describe the condition using eg. range boundaries.
     * @return If present, is a whole sentence. (Starting with capital letter, ending with a full stop.)
     */
    Optional<String> getDescription();

    default boolean test(final LoanWrapper item) {
        return test((Wrapper) item);
    }

    default boolean test(final ParticipationWrapper item) {
        return test((Wrapper) item);
    }

    default boolean test(final InvestmentWrapper item) {
        return test((Wrapper) item);
    }

    @Override
    default boolean test(final Wrapper item) {
        throw new UnsupportedOperationException(item + " in " + this);
    }

    default MarketplaceFilterCondition negate() {
        if (this instanceof NegatingCondition) {
            return ((NegatingCondition) this).getToNegate();
        } else {
            return new NegatingCondition(this);
        }
    }
}
