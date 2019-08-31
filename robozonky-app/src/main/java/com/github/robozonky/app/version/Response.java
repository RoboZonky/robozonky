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

package com.github.robozonky.app.version;

import java.util.Objects;
import java.util.Optional;

final class Response {

    private final String moreRecentStableVersion;
    private final String moreRecentExperimentalVersion;

    private Response() {
        this(null, null);
    }

    private Response(final String moreRecentStableVersion, final String moreRecentExperimentalVersion) {
        this.moreRecentStableVersion = moreRecentStableVersion;
        this.moreRecentExperimentalVersion = moreRecentExperimentalVersion;
    }

    public static Response noMoreRecentVersion() {
        return new Response();
    }

    public static Response moreRecent(final String newStableVersion, final String newExperimentalVersion) {
        return new Response(newStableVersion, newExperimentalVersion);
    }

    public static Response moreRecentExperimental(final String newVersion) {
        return new Response(null, newVersion);
    }

    public static Response moreRecentStable(final String newVersion) {
        return new Response(newVersion, null);
    }

    public Optional<String> getMoreRecentStableVersion() {
        return Optional.ofNullable(moreRecentStableVersion);
    }

    public Optional<String> getMoreRecentExperimentalVersion() {
        return Optional.ofNullable(moreRecentExperimentalVersion);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final Response response = (Response) o;
        return Objects.equals(moreRecentStableVersion, response.moreRecentStableVersion) &&
                Objects.equals(moreRecentExperimentalVersion, response.moreRecentExperimentalVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moreRecentStableVersion, moreRecentExperimentalVersion);
    }

    @Override
    public String toString() {
        return "Response{" +
                "moreRecentExperimentalVersion='" + moreRecentExperimentalVersion + '\'' +
                ", moreRecentStableVersion='" + moreRecentStableVersion + '\'' +
                '}';
    }
}
