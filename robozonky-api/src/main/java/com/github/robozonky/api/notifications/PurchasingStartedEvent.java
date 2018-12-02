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

package com.github.robozonky.api.notifications;

import java.util.Collection;

import com.github.robozonky.api.strategies.ParticipationDescriptor;

/**
 * Fired immediately before the loans are submitted to the investing algorithm.
 * Will eventually be followed by {@link PurchasingCompletedEvent}.
 */
public interface PurchasingStartedEvent extends Financial {

    /**
     * @return Participations on the secondary marketplace that are available for robotic investment.
     */
    Collection<ParticipationDescriptor> getDescriptors();
}
