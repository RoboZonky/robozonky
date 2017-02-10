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

package com.github.triceo.robozonky.internal.api;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

public class DefaultsTest {

    @Rule
    public final RestoreSystemProperties propertiesRestorer = new RestoreSystemProperties();

    @Test
    public void properties() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(Defaults.isDebugEventStorageEnabled()).isFalse();
            softly.assertThat(Defaults.getTokenRefreshBeforeExpirationInSeconds()).isEqualTo(60);
            softly.assertThat(Defaults.getRemoteResourceRefreshIntervalInMinutes()).isEqualTo(5);
            softly.assertThat(Defaults.getCaptchaDelayInSeconds()).isEqualTo(120);
            softly.assertThat(Defaults.getDefaultDryRunBalance()).isEqualTo(-1);
        });
    }

    @Test
    public void userAgent() {
        Assertions.assertThat(Defaults.ROBOZONKY_USER_AGENT)
                .contains(Defaults.ROBOZONKY_URL);
    }

    @Test
    public void hostname() {
        Assertions.assertThat(Defaults.ROBOZONKY_HOST_ADDRESS).isEqualTo(Defaults.getHostAddress());
    }

}
