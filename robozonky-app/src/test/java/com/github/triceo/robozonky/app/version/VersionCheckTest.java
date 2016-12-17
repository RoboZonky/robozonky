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

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class VersionCheckTest {

    @Test
    public void retrieveLatestVersion() throws Exception {
        final Future<VersionIdentifier> version = VersionCheck.retrieveLatestVersion(Executors.newWorkStealingPool());
        Assertions.assertThat(version).isNotNull();
        Assertions.assertThat(version.get()).isNotNull();
    }

    @Test
    public void compareVersions() throws Exception {
        Assertions.assertThat(VersionCheck.isCurrentVersionOlderThan(null)).isFalse(); // just in tests
    }

}
