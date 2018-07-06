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

package com.github.robozonky.cli;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    private final InputStream original = System.in;

    @BeforeEach
    void redirectSystemIn() { // confirm feature setup by Enter keypress
        System.setIn(new ByteArrayInputStream("\n".getBytes(Defaults.CHARSET)));
    }

    @AfterEach
    void restoreSystemIn() {
        System.setIn(original);
    }

    @Test
    void featureFailsInSetup() {
        final Feature f = new Feature() {
            @Override
            public String describe() {
                return null;
            }

            @Override
            public void setup() throws SetupFailedException {
                throw new SetupFailedException("Testing.");
            }

            @Override
            public void test() {
                // won't get here
            }
        };
        final Main main = new Main();
        assertThat(main.apply(f)).isEqualTo(2);
    }

    @Test
    void featureFailsInTest() {
        final Feature f = new Feature() {
            @Override
            public String describe() {
                return null;
            }

            @Override
            public void setup() {
                // do nothing
            }

            @Override
            public void test() throws TestFailedException {
                throw new TestFailedException("Testing.");
            }
        };
        final Main main = new Main();
        assertThat(main.apply(f)).isEqualTo(3);
    }

    @Test
    void helpFeature() {
        final Feature f = new HelpFeature();
        final Main main = new Main();
        assertThat(main.apply(f)).isEqualTo(1);
    }

    @Test
    void standardFeature() throws IOException {
        final File file = File.createTempFile("robozonky-", ".keystore");
        file.delete();
        final Feature f = new MasterPasswordFeature(file, "".toCharArray());
        final Main main = new Main();
        assertThat(main.apply(f)).isEqualTo(0);
    }
}
