/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures that all extensions, available in the current working directory's "extensions/" subdirectory, will be on
 * the classpath.
 *
 * All extension points implementing {@link ServiceLoader} must call this class to retrieve the instance.
 */
public enum ExtensionsManager {

    INSTANCE; // cheap thread-safe singleton

    private Optional<File> findExtensionsFolder() {
        final String folderName = "extensions";
        final File rootFolder = new File(folderName);
        if (rootFolder.exists() && rootFolder.isDirectory()) {
            return Optional.of(rootFolder);
        } else try {
            return Files.walk(new File(System.getProperty("user.dir")).toPath())
                    .map(Path::toFile)
                    .filter(File::isDirectory)
                    .filter(f -> Objects.equals(f.getName(), folderName))
                    .findFirst();
        } catch (final IOException ex) {
            return Optional.empty();
        }
    }

    Collection<URL> retrieveExtensionJars(final File... jars) {
        return Stream.of(jars).map(f -> {
            try {
                return Optional.of(f.toURI().toURL());
            } catch (final MalformedURLException e) {
                this.LOGGER.debug("Skipping extension: '{}'.", f, e);
                return Optional.empty();
            }}).filter(Optional::isPresent)
                .map(o -> (URL)o.get())
                .peek(u -> this.LOGGER.debug("Loading extension: '{}'.", u))
                .collect(Collectors.toSet());
    }

    ClassLoader retrieveExtensionClassLoader(final File extensionsFolder) {
        this.LOGGER.debug("Using extensions folder: '{}'.", extensionsFolder.getAbsolutePath());
        final File[] jars = extensionsFolder.listFiles(file -> file.getPath().toLowerCase().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            return ExtensionsManager.class.getClassLoader();
        }
        final Collection<URL> urls = this.retrieveExtensionJars(jars);
        return new URLClassLoader(urls.toArray(new URL[urls.size()]));
    }

    private ClassLoader retrieveExtensionClassLoader() {
        this.LOGGER.info("Looking up extensions.");
        final Optional<File> extensionsFolder = this.findExtensionsFolder();
        if (!extensionsFolder.isPresent()) {
            this.LOGGER.debug("Extensions folder not found.");
            return ExtensionsManager.class.getClassLoader();
        }
        return this.retrieveExtensionClassLoader(extensionsFolder.get());
    }

    private final Logger LOGGER = LoggerFactory.getLogger(ExtensionsManager.class);
    private final ClassLoader extensionClassLoader;

    ExtensionsManager() {
        this. extensionClassLoader = this.retrieveExtensionClassLoader();
    }

    public <T> ServiceLoader<T> getServiceLoader(final Class<T> serviceClass) {
        this.LOGGER.debug("Retrieving service loader for '{}'.", serviceClass);
        return ServiceLoader.load(serviceClass, this.extensionClassLoader);
    }

}
