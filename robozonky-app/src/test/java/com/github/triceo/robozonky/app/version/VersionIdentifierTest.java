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

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class VersionIdentifierTest {

    @Test
    public void stable() {
        final String version = "1.2.3";
        final VersionIdentifier v = new VersionIdentifier(version);
        Assertions.assertThat(v.getLatestStable()).isEqualTo(version);
        Assertions.assertThat(v.getLatestUnstable()).isEmpty();
    }

    @Test
    public void unstable() {
        final String version = "1.2.3";
        final String version2 = "1.3.0-beta-1";
        final VersionIdentifier v = new VersionIdentifier(version, version2);
        Assertions.assertThat(v.getLatestStable()).isEqualTo(version);
        Assertions.assertThat(v.getLatestUnstable()).isPresent().contains(version2);
    }
}
