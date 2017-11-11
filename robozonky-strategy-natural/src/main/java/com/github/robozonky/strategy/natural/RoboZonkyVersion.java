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

package com.github.robozonky.strategy.natural;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

class RoboZonkyVersion implements Comparable<RoboZonkyVersion> {

    private static final String SNAPSHOT_ID = "-SNAPSHOT";
    private static final Comparator<RoboZonkyVersion> COMPARATOR = Comparator.comparing(RoboZonkyVersion::getMajor)
            .thenComparing(RoboZonkyVersion::getMinor)
            .thenComparing(RoboZonkyVersion::getMicro);

    private final int major, minor, micro;

    public RoboZonkyVersion(final int... digits) {
        this.major = digits[0];
        this.minor = digits[1];
        this.micro = digits[2];
    }

    public RoboZonkyVersion(final String version) {
        this(digits(version));
    }

    private static int[] digits(final String version) {
        final String regular = version.replace(SNAPSHOT_ID, "");
        return Arrays.stream(regular.split("\\Q.\\E"))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getMicro() {
        return micro;
    }

    @Override
    public int compareTo(final RoboZonkyVersion roboZonkyVersion) {
        return COMPARATOR.compare(this, roboZonkyVersion);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoboZonkyVersion)) {
            return false;
        }
        final RoboZonkyVersion that = (RoboZonkyVersion) o;
        return getMajor() == that.getMajor() &&
                getMinor() == that.getMinor() &&
                getMicro() == that.getMicro();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMajor(), getMinor(), getMicro());
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + micro;
    }
}
