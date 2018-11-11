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

package com.github.robozonky.app.version;

import java.util.Optional;

class VersionIdentifier {

    private final String stable, unstable;

    /**
     * Represents a stable version that has no available follow-up unstable versions.
     * @param stable The stable version string.
     */
    VersionIdentifier(final String stable) {
        this(stable, null);
    }

    /**
     * Represents a stable version that has a subsequent unstable version available.
     * @param stable The stable version string.
     * @param unstable The unstable version string.
     */
    VersionIdentifier(final String stable, final String unstable) {
        this.stable = stable;
        this.unstable = unstable;
    }

    public Optional<String> getLatestUnstable() {
        return Optional.ofNullable(this.unstable);
    }

    public String getLatestStable() {
        return this.stable;
    }

    @Override
    public String toString() {
        return "VersionIdentifier{"
                + "stable='" + stable + '\'' +
                ", unstable='" + unstable + '\'' +
                '}';
    }
}
