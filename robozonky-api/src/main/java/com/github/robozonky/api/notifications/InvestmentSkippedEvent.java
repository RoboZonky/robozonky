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

package com.github.robozonky.api.notifications;

import com.github.robozonky.api.strategies.RecommendedLoan;

/**
 * Fired when an event was skipped by the investment algorithm due to CAPTCHA, to be evaluated later after CAPTCHA
 * expires.
 */
public final class InvestmentSkippedEvent extends Event {

    private final RecommendedLoan recommendation;

    public InvestmentSkippedEvent(final RecommendedLoan recommendation) {
        this.recommendation = recommendation;
    }

    public RecommendedLoan getRecommendation() {
        return recommendation;
    }
}
