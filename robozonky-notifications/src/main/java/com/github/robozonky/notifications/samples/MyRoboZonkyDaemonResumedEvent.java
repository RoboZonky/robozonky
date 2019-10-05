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

package com.github.robozonky.notifications.samples;

import com.github.robozonky.api.notifications.RoboZonkyDaemonResumedEvent;

import java.time.OffsetDateTime;

public final class MyRoboZonkyDaemonResumedEvent extends AbstractEvent implements RoboZonkyDaemonResumedEvent {

    private final OffsetDateTime unavailableUntil = OffsetDateTime.now();

    @Override
    public OffsetDateTime getUnavailableSince() {
        return unavailableUntil.minusSeconds(4321);
    }

    @Override
    public OffsetDateTime getUnavailableUntil() {
        return unavailableUntil;
    }
}
