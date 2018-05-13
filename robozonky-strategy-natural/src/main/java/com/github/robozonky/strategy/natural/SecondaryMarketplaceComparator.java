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

package com.github.robozonky.strategy.natural;

import java.util.Comparator;

import com.github.robozonky.api.strategies.ParticipationDescriptor;

/**
 * Participation ordering such that it maximizes the chances the loan is still available on the secondary marketplace
 * when the purchase operation is triggered. In other words, this tries to implement a heuristic of "most popular
 * insured participations first."
 */
class SecondaryMarketplaceComparator implements Comparator<ParticipationDescriptor> {

    private static final Comparator<ParticipationDescriptor>
            BY_TERM = Comparator.comparingInt(p -> p.item().getRemainingInstalmentCount()),
            BY_RECENCY = Comparator.comparing(p -> p.item().getDeadline()),
            BY_REMAINING =
                    Comparator.comparing((ParticipationDescriptor p) -> p.item().getRemainingPrincipal()).reversed(),
            FINAL = BY_TERM.thenComparing(BY_REMAINING).thenComparing(BY_RECENCY);

    @Override
    public int compare(final ParticipationDescriptor p1, final ParticipationDescriptor p2) {
        return FINAL.compare(p1, p2);
    }
}
