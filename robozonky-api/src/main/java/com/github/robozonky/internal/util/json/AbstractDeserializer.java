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

package com.github.robozonky.internal.util.json;

import java.lang.reflect.Type;
import java.util.function.Function;

import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class AbstractDeserializer<T extends Enum<T>> implements JsonbDeserializer<T> {

    protected final Logger logger = LogManager.getLogger(getClass());
    private final Function<String, T> converter;
    private final T defaultValue;

    protected AbstractDeserializer(final Function<String, T> converter, final T defaultValue) {
        this.converter = converter;
        this.defaultValue = defaultValue;
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        try {
            var id = parser.getString();
            return converter.apply(id);
        } catch (final Exception ex) {
            logger.warn("Received unknown value from Zonky: '{}'. This may be a problem, but we continue.",
                    parser.getString());
            return defaultValue;
        }
    }

}
