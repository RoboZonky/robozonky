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

package com.github.robozonky.integrations.stonky;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;
import java.util.stream.Collectors;

import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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

    @Test
    void downloadFromNullStream() {
        assertThat(Util.download((InputStream)null)).isEmpty();
    }

    @Test
    void downloadFromStream() throws IOException {
        final String contents = UUID.randomUUID().toString();
        try (final InputStream s = new ByteArrayInputStream(contents.getBytes(Defaults.CHARSET))) {
            final File target = Util.download(s).orElseThrow(() -> new IllegalStateException("Failed storing file."));
            final String result = Files.readAllLines(target.toPath()).stream().collect(Collectors.joining());
            assertThat(result).isEqualTo(contents);
        }
    }

}
