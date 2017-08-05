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

package com.github.triceo.robozonky.api.notifications;

import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.strategies.RecommendedLoan;

/**
 * Fired immediately after {@link ConfirmationProvider} rejected a given investment.
 */
public final class InvestmentRejectedEvent extends Event {

    private final RecommendedLoan recommendation;
    private final int balance;
    private final String confirmationProviderId;

    public InvestmentRejectedEvent(final RecommendedLoan recommendation, final int balance,
                                   final String confirmationProviderId) {
        this.recommendation = recommendation;
        this.balance = balance;
        this.confirmationProviderId = confirmationProviderId;
    }

    public RecommendedLoan getRecommendation() {
        return recommendation;
    }

    public int getBalance() {
        return balance;
    }

    public String getConfirmationProviderId() {
        return confirmationProviderId;
    }
}
