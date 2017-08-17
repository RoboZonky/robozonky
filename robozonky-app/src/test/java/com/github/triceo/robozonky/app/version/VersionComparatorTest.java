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

package com.github.triceo.robozonky.app.version;

import java.util.Arrays;
import java.util.Collection;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class VersionComparatorTest {

    @Parameterized.Parameters(name = "compare({0}, {1}) == {2}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{"1.0.0", "1.0.1", -1},
                new Object[]{"1.1.0", "1.0.1", 1},
                new Object[]{"1.11.0", "1.1.1", 1},
                new Object[]{"1.1.3", "1.1.21", -1},
                new Object[]{"1.1.4", "1.1.4-CR1", 1},
                new Object[]{"1.1.3-CR1", "1.1.3-BETA1", 1},
                new Object[]{"1.1.2-BETA1", "1.1.2-ALPHA1", 1},
                new Object[]{"1.0.0", "01.0.00", 0}
        );
    }

    @Parameterized.Parameter
    public String left;

    @Parameterized.Parameter(1)
    public String right;

    @Parameterized.Parameter(2)
    public int compareResult;

    @Test
    public void compares() {
        final boolean result = VersionComparator.isSmallerThan(this.left, this.right);
        if (compareResult > -1) {
            Assertions.assertThat(result).isFalse();
        } else {
            Assertions.assertThat(result).isTrue();
        }
    }

    @Test
    public void comparesReverse() {
        final boolean result = VersionComparator.isSmallerThan(this.right, this.left);
        if (compareResult < 1) {
            Assertions.assertThat(result).isFalse();
        } else {
            Assertions.assertThat(result).isTrue();
        }
    }

    @Test
    public void compareNulls() throws Exception {
        Assertions.assertThat(VersionComparator.isSmallerThan(null, null)).isFalse();
    }
}
