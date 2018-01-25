/*
 * Copyright 2017 The RoboZonky Project
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
import java.net.URLClassLoader;
import java.util.ServiceLoader;
import java.util.UUID;

import com.github.robozonky.api.notifications.ListenerService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ExtensionsManagerTest {

    private static final ClassLoader CLASSLOADER = ExtensionsManager.class.getClassLoader();

    @Test
    public void retrieveDefaultClassLoader() {
        final ClassLoader result = ExtensionsManager.INSTANCE.retrieveExtensionClassLoader("");
        Assertions.assertThat(result).isSameAs(ExtensionsManagerTest.CLASSLOADER);
    }

    @Test
    public void noExtensionsWithFolderPresent() {
        final ServiceLoader<ListenerService> result =
                ExtensionsManager.INSTANCE.getServiceLoader(ListenerService.class);
        Assertions.assertThat(result).isNotNull();
    }

    @Test
    public void noExtensionsWithFolderMissing() {
        final ServiceLoader<ListenerService> result =
                ExtensionsManager.INSTANCE.getServiceLoader(ListenerService.class, UUID.randomUUID().toString());
        Assertions.assertThat(result).isNotNull();
    }

    private static File getFolder(final String pathname) {
        final File f = new File(pathname);
        if (!f.exists()) {
            return new File("..", pathname);
        }
        return f;
    }

    @Test
    public void loadJarsFromFolderWithJars() {
        final File f = ExtensionsManagerTest.getFolder("target");
        Assertions.assertThat(ExtensionsManager.INSTANCE.retrieveExtensionClassLoader(f))
                .isInstanceOf(URLClassLoader.class);
    }

    @Test
    public void loadJarsFromFolderWithNoJars() {
        final File f = ExtensionsManagerTest.getFolder("src");
        Assertions.assertThat(ExtensionsManager.INSTANCE.retrieveExtensionClassLoader(f))
                .isInstanceOf(URLClassLoader.class);
    }
}
