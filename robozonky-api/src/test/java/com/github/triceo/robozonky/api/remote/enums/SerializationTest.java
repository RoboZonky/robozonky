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

package com.github.triceo.robozonky.api.remote.enums;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SerializationTest {

    private static String escape(final String toEscape) {
        return '"' + toEscape + '"';
    }

    private static String escape(final int toEscape) {
        return "\"" + toEscape + "\"";
    }

    @Parameterized.Parameters(name = "{0} <=> {1}")
    public static Collection<Object[]> getParameters() {
        final Collection<Object[]> result = new ArrayList<>();
        // process main income type
        for (final MainIncomeType mit : MainIncomeType.values()) {
            result.add(new Object[]{mit, SerializationTest.escape(mit.name())});
        }
        // process purpose
        for (final Purpose p : Purpose.values()) {
            result.add(new Object[]{p, SerializationTest.escape(p.ordinal() + 1)});
        }
        // process region
        for (final Region r : Region.values()) {
            result.add(new Object[]{r, SerializationTest.escape(r.ordinal() + 1)});
        }
        return result;
    }

    @Parameterized.Parameter
    public Object value;
    @Parameterized.Parameter(1)
    public String name;

    @Test
    public void deserialization() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Object result = mapper.readValue(name, value.getClass());
        Assertions.assertThat(result).isSameAs(value);
    }

    @Test
    public void deserializationOfInvalid() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        Assertions.assertThatThrownBy(() -> mapper.readValue(String.valueOf(Integer.MAX_VALUE), value.getClass()))
                .isInstanceOf(RuntimeException.class);
    }
}
