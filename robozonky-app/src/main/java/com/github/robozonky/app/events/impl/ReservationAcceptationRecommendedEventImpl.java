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

import java.math.BigDecimal;

import com.github.robozonky.api.notifications.ReservationAcceptationRecommendedEvent;
import com.github.robozonky.api.remote.entities.sanitized.Reservation;
import com.github.robozonky.api.strategies.RecommendedReservation;

final class ReservationAcceptationRecommendedEventImpl extends AbstractEventImpl
        implements ReservationAcceptationRecommendedEvent {

    private final Reservation reservation;
    private final BigDecimal recommendation;

    public ReservationAcceptationRecommendedEventImpl(final RecommendedReservation recommendation) {
        this.reservation = recommendation.descriptor().item();
        this.recommendation = recommendation.amount();
    }

    @Override
    public BigDecimal getRecommendation() {
        return recommendation;
    }

    public Reservation getReservation() {
        return reservation;
    }
}
