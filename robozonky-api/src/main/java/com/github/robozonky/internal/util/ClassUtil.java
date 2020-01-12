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

package com.github.robozonky.internal.util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

public final class ClassUtil {

    private ClassUtil() {
        // No external instances.
    }

    private static void getAllInterfaces(final Class<?> cls, final Set<Class<?>> interfacesFound) {
        if (cls == null) {
            return;
        }
        for (var i : cls.getInterfaces()) {
            if (interfacesFound.add(i)) {
                getAllInterfaces(i, interfacesFound);
            }
        }
        getAllInterfaces(cls.getSuperclass(), interfacesFound);
    }

    public static Stream<Class<?>> getAllInterfaces(final Class<?> original) {
        if (original == null) {
            return Stream.empty();
        }
        var interfacesFound = new LinkedHashSet<Class<?>>(0);
        getAllInterfaces(original, interfacesFound);
        return interfacesFound.stream();
    }

}
