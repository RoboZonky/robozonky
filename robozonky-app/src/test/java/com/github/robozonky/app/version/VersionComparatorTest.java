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

import java.util.stream.Stream;

import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;

class VersionComparatorTest extends AbstractRoboZonkyTest {

    private static DynamicTest forVersion(final String left, final String right, final int result) {
        return DynamicTest.dynamicTest("left to right", () -> compares(left, right, result));
    }

    private static DynamicTest forVersionReversed(final String left, final String right, final int result) {
        return DynamicTest.dynamicTest("right to left", () -> comparesReverse(left, right, result));
    }

    private static DynamicNode forBoth(final String left, final String right, final int result) {
        return dynamicContainer(left + " v. " + right, Stream.of(
                forVersion(left, right, result),
                forVersionReversed(left, right, result)
        ));
    }

    private static void compares(final String left, final String right, final int compareResult) {
        final boolean result = VersionComparator.isSmallerThan(left, right);
        if (compareResult > -1) {
            assertThat(result).isFalse();
        } else {
            assertThat(result).isTrue();
        }
    }

    private static void comparesReverse(final String left, final String right, final int compareResult) {
        final boolean result = VersionComparator.isSmallerThan(right, left);
        if (compareResult < 1) {
            assertThat(result).isFalse();
        } else {
            assertThat(result).isTrue();
        }
    }

    @TestFactory
    Stream<DynamicNode> versions() {
        return Stream.of(
                forBoth("1.0.0", "1.0.1", -1),
                forBoth("1.1.0", "1.0.1", 1),
                forBoth("1.11.0", "1.1.1", 1),
                forBoth("1.1.3", "1.1.21", -1),
                forBoth("1.1.4", "1.1.4-CR1", 1),
                forBoth("1.1.3-CR1", "1.1.3-BETA1", 1),
                forBoth("1.1.2-BETA1", "1.1.2-ALPHA1", 1),
                forBoth("1.0.0", "01.0.00", 0)
        );
    }

    @Test
    void compareNulls() {
        assertThat(VersionComparator.isSmallerThan(null, null)).isFalse();
    }
}
