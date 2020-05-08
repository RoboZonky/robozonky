/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.util;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

class FileUtilTest {

    private static final File SOME_DIR = new File(UUID.randomUUID()
        .toString());

    @AfterAll
    static void deleteDir() {
        if (SOME_DIR.exists()) {
            SOME_DIR.delete();
        }
    }

    private static File createTempFile() throws IOException {
        Path path = Files.createTempFile("robozonky-", ".tmp");
        File file = path.toFile();
        file.setWritable(false, false);
        file.setReadable(false, false);
        file.setExecutable(false, false);
        return file;
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void permissions() throws IOException {
        File file = createTempFile();
        Path path = file.toPath();
        boolean result = FileUtil.configurePermissions(file, true);
        assertSoftly(softly -> {
            softly.assertThat(path.toFile())
                .canRead()
                .canWrite();
            softly.assertThat(path.toFile()
                .canExecute())
                .isTrue();
            softly.assertThat(result)
                .isTrue();
        });
        boolean result2 = FileUtil.configurePermissions(file, false);
        assertSoftly(softly -> {
            softly.assertThat(path.toFile())
                .canRead()
                .canWrite();
            softly.assertThat(path.toFile()
                .canExecute())
                .isFalse();
            softly.assertThat(result2)
                .isTrue();
        });
    }

    @Test
    void isJarFile() throws IOException {
        Path path = Files.createTempFile("robozonky-", ".jar");
        assumeThat(path).exists();
        File file = path.toFile();
        assertThat(FileUtil.isJarFile(file)).isTrue();
        file.delete();
        assumeThat(FileUtil.isJarFile(file)).isFalse();
    }

    @Test
    void negativeIsJarFile() throws IOException {
        Path path = Files.createTempFile("robozonky-", ".tmp");
        assumeThat(path).exists();
        File file = path.toFile();
        assertThat(FileUtil.isJarFile(file)).isFalse();
        file.delete();
        assumeThat(FileUtil.isJarFile(file)).isFalse();
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
        assertThat(FileUtil.findFolder("target")
            .get()).isPresent();
    }

    @Test
    void lookupNonExistentFolder() {
        assertThat(FileUtil.findFolder(UUID.randomUUID()
            .toString())
            .get()).isEmpty();
    }

    @Test
    void lookupExistingFolderIncludingNonWritableFolder() {
        SOME_DIR.mkdir();
        SOME_DIR.setReadable(false);
        String folderName = UUID.randomUUID()
            .toString();
        assertThat(FileUtil.findFolder(folderName)
            .isRight()).isTrue();
        assertThat(FileUtil.findFolder(folderName)
            .get()).isEmpty();
    }

    @Test
    void lookupNonExistentFolderIncludingNonWritableFolder() {
        SOME_DIR.mkdir();
        SOME_DIR.setReadable(false);
        assertThat(FileUtil.findFolder("target")
            .isRight()).isTrue();
        assertThat(FileUtil.findFolder("target")
            .get()).isPresent();
    }

    @Test
    void lookupSomethingPureWrong() {
        final String old = System.getProperty("user.dir");
        String folderName = UUID.randomUUID()
            .toString();
        System.setProperty("user.dir", folderName);
        assertThat(FileUtil.findFolder(folderName)
            .isLeft()).isTrue();
        System.setProperty("user.dir", old);
    }

    @Test
    void lookupExistingFileAsFolder() {
        assertThat(FileUtil.findFolder("pom.xml")
            .get()).isEmpty();
    }

    @Test
    void someUrls() throws IOException {
        File f = File.createTempFile("robozonky-", ".testing");
        assertThat(FileUtil.filesToUrls(f)).contains(f.toURI()
            .toURL());
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
