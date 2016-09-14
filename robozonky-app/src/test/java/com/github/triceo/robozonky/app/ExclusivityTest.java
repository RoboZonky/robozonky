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

package com.github.triceo.robozonky.app;

import java.io.IOException;

import org.assertj.core.api.SoftAssertions;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class ExclusivityTest {

    @Before
    public void deleteLockFile() {
        Exclusivity.ROBOZONKY_LOCK.setWritable(true);
        Exclusivity.ROBOZONKY_LOCK.delete();
    }

    private static void assertExclusivity(final Exclusivity exclusivity, final boolean isExclusive) {
        final SoftAssertions softly = new SoftAssertions();
        if (isExclusive) {
            softly.assertThat(exclusivity.isEnsured()).isTrue();
            softly.assertThat(Exclusivity.ROBOZONKY_LOCK).exists();
        } else {
            softly.assertThat(exclusivity.isEnsured()).isFalse();
            softly.assertThat(Exclusivity.ROBOZONKY_LOCK).doesNotExist();
        }
        softly.assertAll();
    }

    @Test
    public void acquireAndReacquire() throws IOException {
        final Exclusivity exclusivity = Exclusivity.INSTANCE;
        for (int i = 0; i < 2; i++) {
            System.out.println("Trial " + i);
            exclusivity.ensure();
            ExclusivityTest.assertExclusivity(exclusivity, true);
            exclusivity.waive();
            ExclusivityTest.assertExclusivity(exclusivity, false);
        }
    }

    @Test
    public void doubleAcquireAndWaive() throws IOException {
        final Exclusivity exclusivity = Exclusivity.INSTANCE;
        exclusivity.ensure();
        ExclusivityTest.assertExclusivity(exclusivity, true);
        exclusivity.ensure();
        ExclusivityTest.assertExclusivity(exclusivity, true);
        exclusivity.waive();
        ExclusivityTest.assertExclusivity(exclusivity, false);
        exclusivity.waive();
        ExclusivityTest.assertExclusivity(exclusivity, false);
    }

    @Test
    public void acquireAndWaiveWithPreexistingLockFile() throws IOException {
        Assume.assumeTrue(Exclusivity.ROBOZONKY_LOCK.createNewFile());
        final Exclusivity exclusivity = Exclusivity.INSTANCE;
        exclusivity.ensure();
        ExclusivityTest.assertExclusivity(exclusivity, true);
        exclusivity.waive();
        ExclusivityTest.assertExclusivity(exclusivity, false);
    }

}
