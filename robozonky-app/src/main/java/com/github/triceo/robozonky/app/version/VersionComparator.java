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

import java.io.Serializable;
import java.util.Comparator;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Uses Maven's {@link ComparableVersion} to ensure 100 % compatibility with Maven versioning scheme.
 */
class VersionComparator implements Comparator<String>,
                                   Serializable {

    private static final long serialVersionUID = -4138266839888566436L;

    public static boolean isSmallerThan(final String first, final String second) {
        if (first == null || second == null) { // this is a development snapshot during tests
            return false;
        }
        return new VersionComparator().compare(first, second) < 0;
    }

    @Override
    public int compare(final String s1, final String s2) {
        return new ComparableVersion(s1).compareTo(new ComparableVersion(s2));
    }
}
