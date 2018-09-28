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

package com.github.robozonky.distribution.full;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ClasspathIT {

    private static Optional<Path> findDistributionZip() throws IOException {
        return Files.find(new File("").toPath(), 2, (p, a) -> {
            final String filename = p.getFileName().toString();
            return filename.startsWith("robozonky-distribution-full-") && filename.endsWith(".zip");
        }).findFirst();
    }

    private static Set<String> findDifferences(final Set<String> source, final Set<String> target) {
        return source.stream()
                .filter(s -> !target.contains(s))
                .collect(Collectors.toSet());
    }

    @Test
    void checkClasspath() throws IOException {
        // find the ZIP file
        final Path zip = findDistributionZip().orElseThrow(() -> new IllegalStateException("Nothing was built."));
        final ZipFile zipFile = new ZipFile(zip.toFile());
        // find the main application JAR file
        final ZipEntry appJar = zipFile.stream()
                .filter(e -> !e.isDirectory())
                .filter(e -> e.getName().startsWith("bin/"))
                .filter(e -> e.getName().endsWith("jar"))
                .filter(e -> e.getName().startsWith("bin/robozonky-app"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No app JAR found."));
        // find the libraries
        final Set<String> libraries = zipFile.stream()
                .filter(e -> !e.isDirectory())
                .map(ZipEntry::getName)
                .filter(e -> e.startsWith("bin/"))
                .filter(e -> e.endsWith("jar"))
                .filter(e -> !Objects.equals(e, appJar.getName()))
                .map(e -> e.replaceFirst("\\Qbin/\\E", ""))
                .collect(Collectors.toSet());
        // extract the main app JAR from the ZIP
        final File tmp = File.createTempFile("robozonky-", ".jar");
        tmp.delete();
        Files.copy(zipFile.getInputStream(appJar), tmp.toPath());
        // and analyze the JAR's classpath
        final JarFile jarFile = new JarFile(tmp);
        final String classpath = jarFile.getManifest().getMainAttributes().getValue("Class-Path");
        final Set<String> classpathItems = Stream.of(classpath.split(" ")).collect(Collectors.toSet());
        final Set<String> diff1 = findDifferences(classpathItems, libraries);
        final Set<String> diff2 = findDifferences(libraries, classpathItems);
        assertSoftly(softly -> {
            softly.assertThat(diff1).as("JAR files are on the app classpath but not in the ZIP.").isEmpty();
            softly.assertThat(diff2).as("JAR files are in the ZIP but not on the app classpath.").isEmpty();
        });
    }
}
