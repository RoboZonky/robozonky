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

package com.github.robozonky.app.events.impl;

import java.time.ZonedDateTime;
import java.util.StringJoiner;

import com.github.robozonky.api.notifications.RoboZonkyDaemonResumedEvent;

final class RoboZonkyDaemonResumedEventImpl extends AbstractEventImpl implements RoboZonkyDaemonResumedEvent {

    private final ZonedDateTime unavailableSince;
    private final ZonedDateTime unavailableUntil;

    public RoboZonkyDaemonResumedEventImpl(final ZonedDateTime since, final ZonedDateTime until) {
        this.unavailableSince = since;
        this.unavailableUntil = until;
    }

    @Override
    public ZonedDateTime getUnavailableSince() {
        return unavailableSince;
    }

    @Override
    public ZonedDateTime getUnavailableUntil() {
        return unavailableUntil;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RoboZonkyDaemonResumedEventImpl.class.getSimpleName() + "[", "]")
            .add("super=" + super.toString())
            .add("unavailableSince=" + unavailableSince)
            .add("unavailableUntil=" + unavailableUntil)
            .toString();
    }
}
