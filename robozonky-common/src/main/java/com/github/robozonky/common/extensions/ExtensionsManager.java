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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.ServiceLoader;

import io.vavr.Lazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ensures that all extensions, available in the current working directory's "extensions/" subdirectory, will be on
 * the classpath.
 * <p>
 * All extension points implementing {@link ServiceLoader} must call this class to retrieve the instance.
 */
enum ExtensionsManager {

    INSTANCE; // cheap thread-safe singleton

    private final Logger logger = LogManager.getLogger(ExtensionsManager.class);

    private static boolean isJarFile(final File f) {
        return f.isFile() && f.getPath().toLowerCase().endsWith(".jar");
    }

    ClassLoader retrieveExtensionClassLoader(final File extensionsFolder) {
        logger.debug("Using extensions folder: '{}'.", extensionsFolder.getAbsolutePath());
        final File[] jars = extensionsFolder.listFiles(ExtensionsManager::isJarFile);
        final String jarString = Arrays.toString(jars);
        logger.debug("JARS found: '{}'.", jarString);
        final URL[] urls = FileUtil.filesToUrls(jars).toArray(URL[]::new);
        return new URLClassLoader(urls);
    }

    ClassLoader retrieveExtensionClassLoader(final String folderName) {
        return FileUtil.findFolder(folderName)
                .map(this::retrieveExtensionClassLoader)
                .orElseGet(() -> {
                    logger.debug("Extensions folder not found.");
                    return ExtensionsManager.class.getClassLoader();
                });
    }

    /**
     * @param serviceClass
     * @param <T>
     * @return This is lazy initialized, since if we place the result in a static final variable, and the service
     * loader initialization throws an exception, the parent class will fail to load and we will get a
     * NoClassDefFoundError which gives us no information as to why it happened.
     */
    public <T> Lazy<ServiceLoader<T>> getServiceLoader(final Class<T> serviceClass) {
        return Lazy.of(() -> getServiceLoader(serviceClass, "extensions"));
    }

    <T> ServiceLoader<T> getServiceLoader(final Class<T> serviceClass, final String folderName) {
        logger.debug("Retrieving service loader for '{}'.", serviceClass);
        return ServiceLoader.load(serviceClass, this.retrieveExtensionClassLoader(folderName));
    }

}
