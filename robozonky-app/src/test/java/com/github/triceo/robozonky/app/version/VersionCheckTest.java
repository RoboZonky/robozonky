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
import com.github.triceo.robozonky.app.util.Scheduler;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class VersionCheckTest {

    @Test
    public void retrieveLatestVersion() throws Exception {
        final Refreshable<VersionIdentifier> v = VersionCheck.retrieveLatestVersion(Scheduler.BACKGROUND_SCHEDULER);
        Assertions.assertThat(v.getLatest()).isPresent();
    }

    @Test
    public void compareNulls() throws Exception {
        Assertions.assertThat(VersionCheck.isSmallerThan(null, null)).isFalse();
    }

}
