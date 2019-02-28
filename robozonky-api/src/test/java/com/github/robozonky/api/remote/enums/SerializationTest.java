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

package com.github.robozonky.api.remote.enums;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class SerializationTest {

    private static String escape(final String toEscape) {
        return '"' + toEscape + '"';
    }

    private static String escape(final int toEscape) {
        return "\"" + toEscape + "\"";
    }

    private static void deserialize(final String name, final Object value) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Object result = mapper.readValue(name, value.getClass());
        assertThat(result).isSameAs(value);
    }

    private static void deserialize(final Class<? extends Enum<?>> value) {
        final ObjectMapper mapper = new ObjectMapper();
        assertThatThrownBy(() -> mapper.readValue(String.valueOf(Integer.MAX_VALUE), value))
                .isInstanceOf(RuntimeException.class);
    }

    private static String deserializeTestName(final Enum<?> instance) {
        return instance.getClass().getSimpleName() + '.' + instance;
    }

    @TestFactory
    Collection<DynamicTest> deserialize() {
        final Collection<DynamicTest> tests = new ArrayList<>(0);
        // test deserialization of all income types
        for (final OAuthScope toSerialize : OAuthScope.values()) {
            final String serialized = escape(toSerialize.name());
            tests.add(dynamicTest(deserializeTestName(toSerialize), () -> deserialize(serialized, toSerialize)));
        }
        // test deserialization of all income types
        for (final MainIncomeType toSerialize : MainIncomeType.values()) {
            final String serialized = escape(toSerialize.name());
            tests.add(dynamicTest(deserializeTestName(toSerialize), () -> deserialize(serialized, toSerialize)));
        }
        // test deserialization of all purposes
        for (final Purpose toSerialize : Purpose.values()) {
            final String serialized = escape(toSerialize.ordinal() + 1);
            tests.add(dynamicTest(deserializeTestName(toSerialize), () -> deserialize(serialized, toSerialize)));
        }
        // test deserialization of all regions
        for (final Region toSerialize : Region.values()) {
            final String serialized = escape(toSerialize.ordinal() + 1);
            tests.add(dynamicTest(deserializeTestName(toSerialize), () -> deserialize(serialized, toSerialize)));
        }
        // test that deserialization of invalid value properly fails
        tests.add(dynamicTest("invalid " + Purpose.class.getSimpleName(), () -> deserialize(Purpose.class)));
        return tests;
    }
}
