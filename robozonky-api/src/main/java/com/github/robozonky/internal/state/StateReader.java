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

package com.github.robozonky.internal.state;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Stream;

interface StateReader {

    /**
     * Retrieve a timestamp of when this reader was last updated with content.
     * 
     * @return If present, the timestamp of last update. If missing, this is a brand new reader.
     */
    default Optional<OffsetDateTime> getLastUpdated() {
        return getValue(Constants.LAST_UPDATED_KEY.getValue()).map(OffsetDateTime::parse);
    }

    /**
     * Retrieve a value from this state storage.
     * 
     * @param key Key under which the value was previously stored.
     * @return Present if the storage contains a value for the key, even for the key used to store
     *         {@link #getLastUpdated()}.
     */
    Optional<String> getValue(String key);

    /**
     * Retrieve a value from this state storage using {@link #getValue(String)} and then apply
     * {@link String#split(String)} with a comma (';') as delimiter on the result, converting that to a {@link Stream}.
     * 
     * @param key Key under which the value was previously stored.
     * @return A (possibly empty) stream of values if the key is present, empty otherwise.
     */
    default Optional<Stream<String>> getValues(final String key) {
        return getValues(key, Constants.VALUE_SEPARATOR.getValue());
    }

    /**
     * Retrieve a value from this state storage using {@link #getValue(String)} and then apply
     * {@link String#split(String)} with a given delimiter on the result, converting that to a {@link Stream}.
     * 
     * @param key       Key under which the value was previously stored.
     * @param separator What to split the string by.
     * @return A (possibly empty) stream of values if the key is present, empty otherwise.
     */
    default Optional<Stream<String>> getValues(final String key, final String separator) {
        return getValue(key).map(value -> Stream.of(value.split("\\Q" + separator + "\\E")));
    }

    /**
     * Retrieve all keys associated with this class-specific state storage.
     * 
     * @return Unique key values. Will not include the key used to store {@link #getLastUpdated()}.
     */
    Stream<String> getKeys();
}
