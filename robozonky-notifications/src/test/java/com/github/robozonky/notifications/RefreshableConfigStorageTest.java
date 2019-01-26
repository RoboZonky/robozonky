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

package com.github.robozonky.notifications;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RefreshableConfigStorageTest {

    @Test
    void loadCorrectly() throws IOException {
        final File props = File.createTempFile("robozonky-", ".properties");
        final Properties p = new Properties();
        p.store(Files.newBufferedWriter(props.toPath(), Defaults.CHARSET), "");
        final RefreshableConfigStorage s = new RefreshableConfigStorage(props.toURI().toURL());
        assertThat(s.get()).isPresent();
    }

    @Test
    void loadNonexistent() throws IOException {
        final File props = File.createTempFile("robozonky-", ".properties");
        props.delete();
        final RefreshableConfigStorage s = new RefreshableConfigStorage(props.toURI().toURL());
        assertThat(s.get()).isEmpty();
    }
}
