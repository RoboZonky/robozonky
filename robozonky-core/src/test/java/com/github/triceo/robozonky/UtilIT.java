/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */

package com.github.triceo.robozonky;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class UtilIT {

    @Test
    public void testRoboZonkyBotVersion() {
        final String version = Util.getRoboZonkyVersion();
        Assertions.assertThat(version).isNotEqualTo(Util.ZONKY_VERSION_UNDETECTED);
        Assertions.assertThat(version).isNotEqualTo(Util.ZONKY_VERSION_UNKNOWN);
    }

}
