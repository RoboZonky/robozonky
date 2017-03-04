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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    public static Optional<File> findFolder(final String folderName) {
        final File rootFolder = new File(folderName);
        if (rootFolder.isDirectory()) {
            return Optional.of(rootFolder);
        }
        try {
            return Files.walk(new File(System.getProperty("user.dir")).toPath())
                    .map(Path::toFile)
                    .filter(File::isDirectory)
                    .filter(f -> Objects.equals(f.getName(), folderName))
                    .findFirst();
        } catch (final IOException ex) {
            FileUtils.LOGGER.warn("Exception while walking file tree.", ex);
            return Optional.empty();
        }
    }

    public static Collection<URL> filesToUrls(final File... jars) {
        if (jars == null) {
            return Collections.emptySet();
        }
        return Stream.of(jars)
                .map(f -> {
                    try {
                        return Optional.of(f.toURI().toURL());
                    } catch (final MalformedURLException e) {
                        FileUtils.LOGGER.debug("Skipping file: '{}'.", f, e);
                        return Optional.empty();
                    }
                }).flatMap(o -> o.map(u -> Stream.of((URL)u)).orElse(Stream.empty()))
                .collect(Collectors.toSet());
    }


}
