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

import java.util.Optional;

import com.github.robozonky.api.ReturnCode;

/**
 * Fired before the application forcibly terminates due to an error.
 */
public final class RoboZonkyCrashedEvent extends Event {

    private final ReturnCode returnCode;
    private final Throwable cause;

    public RoboZonkyCrashedEvent(final ReturnCode returnCode, final Throwable cause) {
        this.returnCode = returnCode;
        this.cause = cause;
    }

    public ReturnCode getReturnCode() {
        return this.returnCode;
    }

    public Optional<Throwable> getCause() {
        return Optional.ofNullable(this.cause);
    }
}
