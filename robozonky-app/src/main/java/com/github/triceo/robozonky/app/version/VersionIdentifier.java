/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.version;

import java.util.Optional;

public class VersionIdentifier {

    private final String stable, unstable;

    VersionIdentifier(final String stable) {
        this(stable, null);
    }

    VersionIdentifier(final String stable, final String unstable) {
        this.stable = stable;
        this.unstable = unstable;
    }

    public Optional<String> getLatestUnstable() {
        return this.unstable == null ? Optional.empty() : Optional.of(this.unstable);
    }

    public String getLatestStable() {
        return this.stable;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VersionIdentifier{");
        sb.append("stable='").append(stable).append('\'');
        sb.append(", unstable='").append(unstable).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
