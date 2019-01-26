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

package com.github.robozonky.common.extensions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileUtilTest {

    private static final File SOME_DIR = new File(UUID.randomUUID().toString());

    @AfterAll
    static void deleteDir() {
        if (SOME_DIR.exists()) {
            SOME_DIR.delete();
        }
    }

    @Test
    void processFaultyFiles() throws URISyntaxException {
        final URI uri = new URI("wrongproto://thisiswrong");
        final File f = mock(File.class);
        when(f.toURI()).thenReturn(uri);
        assertThat(FileUtil.filesToUrls(f)).isEmpty();
    }

    @Test
    void lookupExistingFolder() {
        assertThat(FileUtil.findFolder("target")).isPresent();
    }

    @Test
    void lookupNonExistentFolder() {
        assertThat(FileUtil.findFolder(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    void lookupExistingFolderIncludingNonWritableFolder() {
        SOME_DIR.mkdir();
        SOME_DIR.setReadable(false);
        assertThat(FileUtil.findFolder(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    void lookupNonExistentFolderIncludingNonWritableFolder() {
        SOME_DIR.mkdir();
        SOME_DIR.setReadable(false);
        assertThat(FileUtil.findFolder("target")).isPresent();
    }

    @Test
    void lookupSomethingPureWrong() {
        final String old = System.getProperty("user.dir");
        System.setProperty("user.dir", UUID.randomUUID().toString());
        assertThat(FileUtil.findFolder(UUID.randomUUID().toString())).isEmpty();
        System.setProperty("user.dir", old);
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
