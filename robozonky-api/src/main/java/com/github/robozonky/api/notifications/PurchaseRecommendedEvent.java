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

import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.RecommendedParticipation;

/**
 * Fired immediately after {@link PurchaseStrategy} has recommended a particular loan.
 * {@link PurchaseRequestedEvent} may be fired next.
 */
public final class PurchaseRecommendedEvent extends Event {

    private final RecommendedParticipation recommendation;

    public PurchaseRecommendedEvent(final RecommendedParticipation recommendation) {
        this.recommendation = recommendation;
    }

    /**
     * @return The recommendation to be submitted to the investing algorithm.
     */
    public RecommendedParticipation getRecommendation() {
        return this.recommendation;
    }
}
