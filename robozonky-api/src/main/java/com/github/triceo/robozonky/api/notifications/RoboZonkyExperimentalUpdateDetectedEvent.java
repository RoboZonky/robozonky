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

/**
 * Fired when RoboZonky detects that a new unstable (alpha, beta, CR) version is available in Maven Central.
 */
public class RoboZonkyExperimentalUpdateDetectedEvent extends Event {

    private final String newVersion;

    public RoboZonkyExperimentalUpdateDetectedEvent(final String newVersion) {
        this.newVersion = newVersion;
    }

    public final String getNewVersion() {
        return newVersion;
    }
}
