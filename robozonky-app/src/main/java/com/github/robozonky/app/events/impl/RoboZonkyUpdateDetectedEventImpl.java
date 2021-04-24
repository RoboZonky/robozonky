/*
 * Copyright 2021 The RoboZonky Project
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

import java.util.StringJoiner;

import com.github.robozonky.api.notifications.Release;
import com.github.robozonky.api.notifications.RoboZonkyUpdateDetectedEvent;

final class RoboZonkyUpdateDetectedEventImpl extends AbstractEventImpl implements RoboZonkyUpdateDetectedEvent {

    private final Release newVersion;

    public RoboZonkyUpdateDetectedEventImpl(final Release newVersion) {
        this.newVersion = newVersion;
    }

    @Override
    public final Release getNewVersion() {
        return newVersion;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RoboZonkyUpdateDetectedEventImpl.class.getSimpleName() + "[", "]")
            .add("super=" + super.toString())
            .add("newVersion='" + newVersion + "'")
            .toString();
    }
}
