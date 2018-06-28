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

package com.github.robozonky.common.state;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface StateModifier<T> {

    /**
     * Associate a value with a key in the underlying {@link StateStorage}.
     * @param key Key to look up the value with.
     * @param value Value to associate with the key, overwriting anything previously set there.
     * @return This.
     */
    StateModifier<T> put(final String key, final String value);

    /**
     * Associate a value with a key in the underlying {@link StateStorage}, first joining all the strings to one using
     * a single ";" character. Such values can then be read back with {@link StateReader#getValues(String)}.
     * @param key Key to look up the value with.
     * @param values Values to associate with the key, overwriting anything previously set there.
     * @return This.
     */
    default StateModifier<T> put(final String key, final Stream<String> values) {
        return put(key, values, StateReader.DEFAULT_VALUE_SEPARATOR);
    }

    /**
     * Associate a value with a key in the underlying {@link StateStorage}, first joining all the strings to one using
     * a given delimiter. Such values can then be read with {@link StateReader#getValues(String, char)}.
     * @param key Key to look up the value with.
     * @param values Values to associate with the key, overwriting anything previously set there.
     * @param separator Used to merge the values into one string that will be associated with the key.
     * @return This.
     */
    default StateModifier<T> put(final String key, final Stream<String> values, final char separator) {
        return put(key, values.collect(Collectors.joining(Character.toString(separator))));
    }

    /**
     * Remove value associated with the key from underlying {@link StateStorage}.
     * @param key Key to look up the value with.
     * @return This.
     */
    StateModifier<T> remove(final String key);
}
