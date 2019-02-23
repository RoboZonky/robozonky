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

import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class VersionIdentifierTest extends AbstractRoboZonkyTest {

    @Test
    void stable() {
        final String version = "1.2.3";
        final VersionIdentifier v = new VersionIdentifier(version);
        assertThat(v.getLatestStable()).isEqualTo(version);
        assertThat(v.getLatestUnstable()).isEmpty();
    }

    @Test
    void unstable() {
        final String version = "1.2.3";
        final String version2 = "1.3.0-beta-1";
        final VersionIdentifier v = new VersionIdentifier(version, version2);
        assertThat(v.getLatestStable()).isEqualTo(version);
        assertThat(v.getLatestUnstable()).isPresent().contains(version2);
    }
}
