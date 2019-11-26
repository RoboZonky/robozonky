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

package com.github.robozonky.app.events.impl;

import java.util.StringJoiner;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.strategies.Descriptor;
import com.github.robozonky.api.strategies.Recommended;

abstract class AbstractRecommendationBasedEventImpl<T extends Recommended<T, S, X>, S extends Descriptor<T, S, X>, X>
        extends AbstractEventImpl {

    private final X recommending;
    private final Loan loan;
    private final Money recommendation;

    protected AbstractRecommendationBasedEventImpl(final T recommendation) {
        this.recommending = recommendation.descriptor().item();
        this.loan = recommendation.descriptor().related();
        this.recommendation = recommendation.amount();
    }

    public Loan getLoan() {
        return loan;
    }

    protected X getRecommending() {
        return recommending;
    }

    public Money getRecommendation() {
        return recommendation;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("super=" + super.toString())
                .add("loan=" + loan)
                .add("recommending=" + recommending)
                .add("recommendation=" + recommendation)
                .toString();
    }
}
