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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.github.triceo.robozonky.api.Defaults;

/**
 * Class to perform version checks of the current version against the latest released version.
 */
public class VersionCheck {

    /**
     * Will retrieve the latest version available in Maven Central. Executes in single thread executor.
     * @param e Executor service to use for executing the HTTP request.
     * @return Latest known release version of RoboZonky, as a {@link Future} to be retrieved later.
     */
    public static Future<VersionIdentifier> retrieveLatestVersion(final ExecutorService e) {
        return e.submit(new VersionRetriever());
    }

    /**
     * Check the current version of RoboZonky against a given other version string.
     * @param latestVersion Version string to compare to.
     * @return True if current version is older than the version in question.
     */
    public static boolean isCurrentVersionOlderThan(final String latestVersion) {
        return VersionCheck.isSmallerThan(Defaults.ROBOZONKY_VERSION, latestVersion);
    }

    static boolean isSmallerThan(final String first, final String second) {
        if (first == null || second == null) { // this is a development snapshot during tests
            return false;
        }
        return new VersionComparator().compare(first, second) < 0;
    }

}
