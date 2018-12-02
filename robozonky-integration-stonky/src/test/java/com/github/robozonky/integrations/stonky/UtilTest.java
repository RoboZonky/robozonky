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

package com.github.robozonky.integrations.stonky;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtilTest extends AbstractRoboZonkyTest {

    @Test
    void transport() {
        assertThat(Util.createTransport()).isNotNull();
    }

    @Test
    void downloadFromWrongUrl() throws MalformedURLException {
        final URL url = new URL("http://" + UUID.randomUUID());
        assertThat(Util.download(url)).isEmpty();
    }

}
