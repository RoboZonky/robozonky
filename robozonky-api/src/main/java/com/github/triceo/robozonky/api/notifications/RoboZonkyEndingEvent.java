/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.notifications;

import com.github.triceo.robozonky.api.ReturnCode;

/**
 * Fired before the application shuts down, provided {@link RoboZonkyInitializedEvent} was fired before.
 */
public final class RoboZonkyEndingEvent extends Event {

    private final ReturnCode returnCode;

    public RoboZonkyEndingEvent(final ReturnCode returnCode) {
        this.returnCode = returnCode;
    }

    /**
     *
     * @return The error code that RoboZonky will end with.
     */
    public ReturnCode getReturnCode() {
        return this.returnCode;
    }

}
