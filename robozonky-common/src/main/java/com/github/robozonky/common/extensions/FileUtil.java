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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class FileUtil {

    private static final Logger LOGGER = LogManager.getLogger(FileUtil.class);

    private FileUtil() {
        // no instances
    }

    public static Optional<File> findFolder(final String folderName) {
        final Path root = new File(System.getProperty("user.dir")).toPath();
        return Try.withResources(() -> Files.find(root, 1, (path, attr) -> attr.isDirectory()))
                .of(s -> s.map(Path::toFile)
                        .filter(f -> Objects.equals(f.getName(), folderName))
                        .findFirst())
                .getOrElseGet(ex -> {
                    LOGGER.warn("Exception while walking file tree.", ex);
                    return Optional.empty();
                });
    }

    public static Stream<URL> filesToUrls(final File... jars) {
        if (jars == null) {
            throw new IllegalArgumentException("Null");
        }
        return Stream.of(jars)
                .map(f -> {
                    try {
                        return Optional.of(f.toURI().toURL());
                    } catch (final MalformedURLException e) {
                        LOGGER.debug("Skipping file: '{}'.", f, e);
                        return Optional.<URL>empty();
                    }
                }).flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
    }
}
