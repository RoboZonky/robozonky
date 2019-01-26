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

package com.github.robozonky.common.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.robozonky.internal.api.Defaults;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class InterceptingInputStreamTest {

    @Test
    void standard() throws IOException {
        final String contents = UUID.randomUUID().toString();
        final InputStream s = new ByteArrayInputStream(contents.getBytes());
        try (final InterceptingInputStream s2 = new InterceptingInputStream(s)) {
            // first check content has been intercepted
            assertThat(s2.getContents()).isEqualTo(contents);
            // then check content is still available
            assertThat(IOUtils.toString(s2, Defaults.CHARSET)).isEqualTo(contents);
        }
    }

    @Test
    void tooLong() throws IOException {
        final int maxLength = 1024;
        final int uuidLength = UUID.randomUUID().toString().length();
        final String contents = IntStream.range(0, (maxLength / uuidLength) + 2)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.joining());
        final InputStream s = new ByteArrayInputStream(contents.getBytes());
        try (final InterceptingInputStream s2 = new InterceptingInputStream(s)) {
            assertThat(s2.getContents()).endsWith("...more...");
        }
    }
}

