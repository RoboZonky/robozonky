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
import com.github.robozonky.api.notifications.LoanRecommendedEvent;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.strategies.RecommendedLoan;

final class LoanRecommendedEventImpl extends AbstractEventImpl implements LoanRecommendedEvent {

    private final Loan loan;
    private final Money recommendation;

    public LoanRecommendedEventImpl(final RecommendedLoan recommendation) {
        this.loan = recommendation.descriptor().item();
        this.recommendation = recommendation.amount();
    }

    /**
     * @return The recommendation to be submitted to the investing algorithm.
     */
    @Override
    public Money getRecommendation() {
        return recommendation;
    }

    @Override
    public Loan getLoan() {
        return loan;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LoanRecommendedEventImpl.class.getSimpleName() + "[", "]")
                .add("super=" + super.toString())
                .add("loan=" + loan)
                .add("recommendation=" + recommendation)
                .toString();
    }
}
