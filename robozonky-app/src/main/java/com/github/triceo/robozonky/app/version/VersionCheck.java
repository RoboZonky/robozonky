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

package com.github.triceo.robozonky.app.version;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.util.Scheduler;

/**
 * Class to perform version checks of the current version against the latest released version.
 */
public class VersionCheck {

    /**
     * Will retrieve the latest version available in Maven Central. Executes in single thread executor.
     * @param s Scheduler to use for executing the HTTP request.
     * @return Latest known release version of RoboZonky.
     */
    public static Refreshable<VersionIdentifier> retrieveLatestVersion(final Scheduler s) {
        final VersionRetriever r = new VersionRetriever();
        s.submit(r);
        return r;
    }

    public static boolean isSmallerThan(final String first, final String second) {
        if (first == null || second == null) { // this is a development snapshot during tests
            return false;
        }
        return new VersionComparator().compare(first, second) < 0;
    }

}
