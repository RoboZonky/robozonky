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

package com.github.robozonky.cli;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class NotificationTestingFeatureTest {

    @Test
    void noNotifications() throws MalformedURLException, SetupFailedException {
        final String username = UUID.randomUUID().toString();
        final URL url = new URL("http://localhost");
        final Feature f = new NotificationTestingFeature(username, url);
        f.setup();
        assertThatThrownBy(f::test).isInstanceOf(TestFailedException.class);
    }

}
