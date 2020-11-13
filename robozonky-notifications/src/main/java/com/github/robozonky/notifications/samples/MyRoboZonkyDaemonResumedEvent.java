/*
 * Copyright 2020 The RoboZonky Project
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

import java.time.ZonedDateTime;

import com.github.robozonky.api.notifications.RoboZonkyDaemonResumedEvent;
import com.github.robozonky.internal.test.DateUtil;

public final class MyRoboZonkyDaemonResumedEvent extends AbstractEvent implements RoboZonkyDaemonResumedEvent {

    private final ZonedDateTime unavailableUntil = DateUtil.zonedNow();

    @Override
    public ZonedDateTime getUnavailableSince() {
        return unavailableUntil.minusSeconds(4321);
    }

    @Override
    public ZonedDateTime getUnavailableUntil() {
        return unavailableUntil;
    }
}
