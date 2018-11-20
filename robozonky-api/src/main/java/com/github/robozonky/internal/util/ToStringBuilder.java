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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ToStringBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToStringBuilder.class);
    private final ReflectionToStringBuilder builder;

    private ToStringBuilder(final Object o, final String... excludeFields) {
        final String[] fieldExclusions = Stream.concat(Stream.of("password"), Arrays.stream(excludeFields))
                .distinct()
                .toArray(String[]::new);
        this.builder = new CustomReflectionToStringBuilder(o).setExcludeFieldNames(fieldExclusions);
    }

    public static LazyInitialized<String> createFor(final Object o, final String... excludeFields) {
        return LazyInitialized.create(() -> {
            try {
                return new ToStringBuilder(o, excludeFields).toString();
            } catch (final Exception ex) {
                LOGGER.debug("Error creating toString().", ex);
                return "ERROR";
            }
        });
    }

    @Override
    public String toString() {
        return this.builder.toString();
    }

    private static class CustomReflectionToStringBuilder extends ReflectionToStringBuilder {

        private static final int MAX_STRING_LENGTH = 70;

        // ignore passwords and loggers
        private static final Set<Class<?>> IGNORED_TYPES = new HashSet<>(Arrays.asList(char[].class, Logger.class));

        public CustomReflectionToStringBuilder(final Object o) {
            super(o);
        }

        @Override
        protected boolean accept(final Field field) {
            return super.accept(field)
                    && !Modifier.isStatic(field.getModifiers())
                    && !IGNORED_TYPES.contains(field.getType());
        }

        @Override
        protected Object getValue(final Field field) throws IllegalAccessException {
            final Object value = super.getValue(field);
            if (value instanceof String) { // long strings will get truncated
                final String stringValue = (String) value;
                return StringUtils.abbreviate(stringValue, MAX_STRING_LENGTH);
            } else {
                return value;
            }
        }
    }
}
