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

package com.github.robozonky.common.secrets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.security.KeyStoreException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class KeyStoreHandlerTest {

    private static File TARGET;
    private static final char[] PASSWORD = "ABš CD1234-".toCharArray();

    @BeforeAll
    static void createTempFile() {
        try {
            KeyStoreHandlerTest.TARGET = File.createTempFile("robozonky-", ".keystore");
            KeyStoreHandlerTest.TARGET.delete();
            assertThat(KeyStoreHandlerTest.TARGET).doesNotExist();
        } catch (final IOException e) {
            fail("Could not create temp file.", e);
        }
    }

    @Test
    void nullFileOnCreate() {
        assertThatThrownBy(() -> KeyStoreHandler.create(null))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void nullFileOnOpen() {
        assertThatThrownBy(() -> KeyStoreHandler.open(null))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void preexistingFileOnCreate() throws IOException {
        File f = File.createTempFile("robozonky-", ".keystore");
        assertThatThrownBy(() -> KeyStoreHandler.create(f))
                .isInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    void noneexistentFileOnOpen() throws IOException {
        File f = File.createTempFile("robozonky-", ".keystore");
        f.delete();
        assertThatThrownBy(() -> KeyStoreHandler.open(f)).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void storeSomethingAndReadIt() throws IOException, KeyStoreException {
        final String key = "abc";
        final String value = "def";
        // store
        assertThat(KeyStoreHandlerTest.TARGET).doesNotExist();
        final KeyStoreHandler ksh = KeyStoreHandler.create(KeyStoreHandlerTest.TARGET, KeyStoreHandlerTest.PASSWORD);
        assertThat(ksh.isDirty()).isFalse();
        assertThat(KeyStoreHandlerTest.TARGET).exists();
        final boolean isStored = ksh.set(key, value.toCharArray());
        assertThat(isStored).isTrue();
        assertThat(ksh.isDirty()).isTrue();
        ksh.save();
        assertThat(ksh.isDirty()).isFalse();
        // read same
        assertThat(ksh.get(key)).isPresent();
        assertThat(ksh.get(key)).contains(value.toCharArray());
        // read new
        final KeyStoreHandler ksh2 = KeyStoreHandler.open(KeyStoreHandlerTest.TARGET, KeyStoreHandlerTest.PASSWORD);
        assertThat(ksh2.isDirty()).isFalse();
        assertThat(ksh2.get(key)).isPresent();
        assertThat(ksh2.get(key)).contains(value.toCharArray());
        // delete
        final boolean isDeleted = ksh2.delete(key);
        assertThat(isDeleted).isTrue();
        assertThat(ksh2.isDirty()).isTrue();
        assertThat(ksh2.get(key)).isEmpty();
    }
}
