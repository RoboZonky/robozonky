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

package com.github.robozonky.internal.util;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Collections.unmodifiableSortedMap;

/**
 * May be replaced by Map.* when we upgrade to JDK 11.
 */
public final class Maps {

    private Maps() { // no instances

    }

    public static <K, V> Map.Entry<K, V> entry(final K k, final V v) {
        return new AbstractMap.SimpleImmutableEntry<>(k, v);
    }

    @SafeVarargs
    public static <K, V> Map<K, V> ofEntries(final Map.Entry<? extends K, ? extends V>... entries) {
        // not implemented as a stream with Collectors.toMap(), since that will throw NPE on null values
        final Map<K, V> result = new LinkedHashMap<>(entries.length);
        for (final Map.Entry<? extends K, ? extends V> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <K, V> SortedMap<K, V> ofEntriesSorted(final Map.Entry<? extends K, ? extends V>... entries) {
        final SortedMap<K, V> sorted = new TreeMap<>(ofEntries(entries));
        return unmodifiableSortedMap(sorted);
    }
}
