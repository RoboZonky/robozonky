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

package com.github.triceo.robozonky.remote;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

interface BaseEntity {

    static Logger getLogger(final Class<?> classToLog) {
        return LoggerFactory.getLogger(classToLog);
    }

    @JsonAnyGetter
    default void handleUnknownGetter(final String key) {
        BaseEntity.getLogger(this.getClass()).warn("Trying to get value of unknown property '{}'."
                + " Indicates an unexpected API change, RoboZonky may misbehave.", key);
    }

    @JsonAnySetter
    default void handleUnknownSetter(final String key, final Object value) {
        BaseEntity.getLogger(this.getClass()).warn("Trying to set value '{}' to an unknown property '{}'."
                + " Indicates an unexpected API change, RoboZonky may misbehave.", value, key);
    }

}
