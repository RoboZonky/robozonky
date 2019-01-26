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

package com.github.robozonky.common.state;

import java.io.File;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class FileBackedStateStorageTest {

    private static final Logger LOGGER = LogManager.getLogger(FileBackedStateStorageTest.class);

    private final File f = new File(UUID.randomUUID().toString());
    private final FileBackedStateStorage s = new FileBackedStateStorage(f);

    @Test
    @DisplayName("Storing empty string means no value.")
    void valueEmpty() {
        s.setValue("section", "key", "     ");
        assertThat(s.getValue("section", "key")).isEmpty();
    }

    @Test
    @DisplayName("No sections are available.")
    void sectionsDoNotThrow() {
        assertThat(s.getSections()).isEmpty();
    }

    @Test
    @DisplayName("Random section has no keys.")
    void keysDoNotThrow() {
        assertThat(s.getKeys(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    @DisplayName("Random section can be unset without throwing.")
    void sectionUnset() {
        s.unsetValues(UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("Random value can be unset without throwing.")
    void valueUnset() {
        s.unsetValue(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("Random key has no value.")
    void valuesDoNotThrow() {
        assertThat(s.getValue(UUID.randomUUID().toString(), UUID.randomUUID().toString())).isEmpty();
    }

    @AfterEach
    @BeforeEach
    void deleteState() {
        s.destroy();
        LOGGER.info("State destroyed.");
    }

    @Nested
    @DisplayName("When value is added to a section")
    class StorageTesting {

        @BeforeEach
        void setValue() {
            s.setValue("section", "key", "value");
        }

        @Test
        @DisplayName("is properly persisted without storing.")
        void persistence() { // strings are not reused to ensure that this typical use case works
            assertSoftly(softly -> {
                softly.assertThat(s.getValue("section", "key")).contains("value");
                softly.assertThat(s.getKeys("section")).containsOnly("key");
                softly.assertThat(s.getSections()).containsOnly("section");
            });
        }

        @Test
        @DisplayName("can be removed as a single value.")
        void unsetValue() {
            s.unsetValue("section", "key");
            assertSoftly(softly -> {
                softly.assertThat(s.getValue("section", "key")).isEmpty();
                softly.assertThat(s.getKeys("section")).isEmpty();
                softly.assertThat(s.getSections()).containsOnly("section");
            });
        }

        @Test
        @DisplayName("can be removed with its section.")
        void unsetSection() {
            s.unsetValues("section");
            assertSoftly(softly -> {
                softly.assertThat(s.getValue("section", "key")).isEmpty();
                softly.assertThat(s.getKeys("section")).isEmpty();
                softly.assertThat(s.getSections()).isEmpty();
            });
        }

        @Test
        @DisplayName("doesn't interfere with same in another section.")
        void nonInterference() { // strings are not reused to ensure that this typical use case works
            s.setValue("section2", "key", "value2");
            assertSoftly(softly -> {
                softly.assertThat(s.getValue("section", "key")).contains("value");
                softly.assertThat(s.getValue("section2", "key")).contains("value2");
                softly.assertThat(s.getKeys("section")).containsOnly("key");
                softly.assertThat(s.getKeys("section2")).containsOnly("key");
                softly.assertThat(s.getSections()).containsOnly("section", "section2");
            });
        }

        @Nested
        @DisplayName("is stored and the file is re-read")
        class SurvivalTesting {

            @BeforeEach
            void storeValues() {
                s.store();
            }

            @Test
            @DisplayName("value survives into a new storage instance.")
            void survival() { // strings are not reused to ensure that this typical use case works
                final FileBackedStateStorage s2 = new FileBackedStateStorage(f);
                assertSoftly(softly -> {
                    softly.assertThat(s2.getValue("section", "key")).contains("value");
                    softly.assertThat(s2.getKeys("section")).containsOnly("key");
                    softly.assertThat(s2.getSections()).containsOnly("section");
                });
            }

            @Test
            @DisplayName("unstored value can not be seen by the new instance.")
            void noWriteThrough() { // strings are not reused to ensure that this typical use case works
                s.setValue("section", "key2", "value2"); // store() not called
                final FileBackedStateStorage s2 = new FileBackedStateStorage(f);
                assertSoftly(softly -> { // only the stored values are available
                    softly.assertThat(s2.getValue("section", "key")).contains("value");
                    softly.assertThat(s2.getKeys("section")).containsOnly("key");
                    softly.assertThat(s2.getSections()).containsOnly("section");
                });
            }
        }
    }
}
