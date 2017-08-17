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

package com.github.triceo.robozonky.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.security.KeyStoreException;

import com.github.triceo.robozonky.common.secrets.KeyStoreHandler;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

public class KeyStoreHandlerTest {

    private static File TARGET;
    private static final char[] PASSWORD = "ABš CD1234-".toCharArray();

    @BeforeClass
    public static void createTempFile() {
        try {
            KeyStoreHandlerTest.TARGET = File.createTempFile("robozonky-", ".keystore");
            KeyStoreHandlerTest.TARGET.delete();
            Assertions.assertThat(KeyStoreHandlerTest.TARGET).doesNotExist();
        } catch (final IOException e) {
            Assertions.fail("Could not create temp file.", e);
        }
    }

    @Test
    public void nullFileOnCreate() {
        Assertions.assertThatThrownBy(() -> KeyStoreHandler.create(null))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void nullFileOnOpen() {
        Assertions.assertThatThrownBy(() -> KeyStoreHandler.open(null))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void preexistingFileOnCreate() throws IOException {
        File f = File.createTempFile("robozonky-", ".keystore");
        Assertions.assertThatThrownBy(() -> KeyStoreHandler.create(f))
                .isInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    public void noneexistentFileOnOpen() throws IOException {
        File f = File.createTempFile("robozonky-", ".keystore");
        f.delete();
        Assertions.assertThatThrownBy(() -> KeyStoreHandler.open(f)).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void storeSomethingAndReadIt() throws IOException, KeyStoreException {
        final String key = "abc";
        final String value = "def";
        // store
        Assertions.assertThat(KeyStoreHandlerTest.TARGET).doesNotExist();
        final KeyStoreHandler ksh = KeyStoreHandler.create(KeyStoreHandlerTest.TARGET, KeyStoreHandlerTest.PASSWORD);
        Assertions.assertThat(ksh.isDirty()).isFalse();
        Assertions.assertThat(KeyStoreHandlerTest.TARGET).exists();
        final boolean isStored = ksh.set(key, value.toCharArray());
        Assertions.assertThat(isStored).isTrue();
        Assertions.assertThat(ksh.isDirty()).isTrue();
        ksh.save();
        Assertions.assertThat(ksh.isDirty()).isFalse();
        // read same
        Assertions.assertThat(ksh.get(key)).isPresent();
        Assertions.assertThat(ksh.get(key)).contains(value.toCharArray());
        // read new
        final KeyStoreHandler ksh2 = KeyStoreHandler.open(KeyStoreHandlerTest.TARGET, KeyStoreHandlerTest.PASSWORD);
        Assertions.assertThat(ksh2.isDirty()).isFalse();
        Assertions.assertThat(ksh2.get(key)).isPresent();
        Assertions.assertThat(ksh2.get(key)).contains(value.toCharArray());
        // delete
        final boolean isDeleted = ksh2.delete(key);
        Assertions.assertThat(isDeleted).isTrue();
        Assertions.assertThat(ksh2.isDirty()).isTrue();
        Assertions.assertThat(ksh2.get(key)).isEmpty();
    }
}
