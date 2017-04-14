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

import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

/**
 * Created by lpetrovi on 14.4.17.
 */
public class SettingsTest {

    @Rule
    public final RestoreSystemProperties propertiesRestorer = new RestoreSystemProperties();

    @Test
    public void properties() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(Settings.INSTANCE.get("user.dir", "")).isNotEqualTo("");
            softly.assertThat(Settings.INSTANCE.get(UUID.randomUUID().toString(), ""))
                    .isEqualTo("");
            softly.assertThat(Settings.INSTANCE.isDebugEventStorageEnabled()).isFalse();
            softly.assertThat(Settings.INSTANCE.getTokenRefreshBeforeExpirationInSeconds()).isEqualTo(60);
            softly.assertThat(Settings.INSTANCE.getRemoteResourceRefreshIntervalInMinutes()).isEqualTo(5);
            softly.assertThat(Settings.INSTANCE.getCaptchaDelayInSeconds()).isEqualTo(120);
            softly.assertThat(Settings.INSTANCE.getDefaultDryRunBalance()).isEqualTo(-1);
        });
    }


}
