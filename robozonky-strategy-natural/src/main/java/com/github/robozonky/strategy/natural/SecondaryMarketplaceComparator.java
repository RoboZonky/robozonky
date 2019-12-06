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

package com.github.robozonky.strategy.natural;

import java.util.Comparator;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ParticipationDescriptor;

class SecondaryMarketplaceComparator implements Comparator<ParticipationDescriptor> {

    private final Comparator<Participation> comparator;

    public SecondaryMarketplaceComparator(Comparator<Rating> ratingByDemandComparator) {
        this.comparator = Comparator.comparing(Participation::getRating, ratingByDemandComparator);
    }

    @Override
    public int compare(final ParticipationDescriptor p1, final ParticipationDescriptor p2) {
        return comparator.compare(p1.item(), p2.item());
    }
}
