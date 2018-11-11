/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.common;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ZonkyScopeTest {

    @Test
    void defaults() {
        assertThat(ZonkyScope.getDefault()).isEqualTo(ZonkyScope.APP);
        assertThat(ZonkyScope.APP.getId()).isEqualTo(ZonkyApiToken.SCOPE_APP_WEB_STRING);
        assertThat(ZonkyScope.FILES.getId()).isEqualTo(ZonkyApiToken.SCOPE_FILE_DOWNLOAD_STRING);
    }

}
