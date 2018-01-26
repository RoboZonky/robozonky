/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileUtilTest {

    @Test
    void processFaultyFiles() throws URISyntaxException {
        final URI uri = new URI("wrongproto://thisiswrong");
        final File f = mock(File.class);
        when(f.toURI()).thenReturn(uri);
        assertThat(FileUtil.filesToUrls(f)).isEmpty();
    }

    @Test
    void lookupNonexistentFolder() {
        assertThat(FileUtil.findFolder("target")).isPresent();
    }

    @Test
    void lookupExistingFileAsFolder() {
        assertThat(FileUtil.findFolder("pom.xml")).isEmpty();
    }

    @Test
    void someUrls() throws IOException {
        File f = File.createTempFile("robozonky-", ".testing");
        assertThat(FileUtil.filesToUrls(f)).contains(f.toURI().toURL());
    }

    @Test
    void nullUrls() {
        assertThatThrownBy(() -> FileUtil.filesToUrls((File[]) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyUrls() {
        assertThat(FileUtil.filesToUrls(new File[0])).isEmpty();
    }
}
