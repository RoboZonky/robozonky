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

package com.github.robozonky.cli.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.util.FileUtil;

public final class Util {

    private static final Logger LOGGER = LogManager.getLogger(Util.class);

    private Util() {
        // no instances
    }

    public static void writeOutProperties(final Properties properties, final File target) throws IOException {
        writeOutProperties(properties, target.toPath());
    }

    public static void writeOutProperties(final Properties properties, final Path target) throws IOException {
        try (var writer = Files.newBufferedWriter(target, Defaults.CHARSET)) {
            FileUtil.configurePermissions(target.toFile(), false);
            properties.store(writer, Defaults.ROBOZONKY_USER_AGENT);
            LOGGER.debug("Written properties to {}.", target);
        }
    }

    public static void copy(final Path from, final Path to) throws IOException {
        LOGGER.debug("Copying {} to {}", from, to);
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        FileUtil.configurePermissions(to.toFile(), false);
    }

}
