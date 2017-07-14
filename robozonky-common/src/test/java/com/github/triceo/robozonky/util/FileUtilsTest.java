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

package com.github.triceo.robozonky.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class FileUtilsTest {

    @Test
    public void processFaultyFiles() {
        final File f = Mockito.mock(File.class);
        Mockito.doThrow(MalformedURLException.class).when(f).toURI();
        Assertions.assertThat(FileUtils.filesToUrls(f)).isEmpty();
    }

    @Test
    public void lookupNonexistentFolder() {
        Assertions.assertThat(FileUtils.findFolder("target")).isPresent();
    }

    @Test
    public void lookupExistingFileAsFolder() {
        Assertions.assertThat(FileUtils.findFolder("pom.xml")).isEmpty();
    }

    @Test
    public void someUrls() throws IOException {
        File f = File.createTempFile("robozonky-", ".testing");
        Assertions.assertThat(FileUtils.filesToUrls(f)).contains(f.toURI().toURL());
    }

    @Test
    public void nullUrls() {
        Assertions.assertThatThrownBy(() -> FileUtils.filesToUrls((File[]) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void emptyUrls() {
        Assertions.assertThat(FileUtils.filesToUrls(new File[0])).isEmpty();
    }

}
