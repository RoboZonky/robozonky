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

package com.github.robozonky.common.remote;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SelectTest {

    @Test
    void contains() {
        final String fieldName = "field";
        final String value = "value";
        final Select select = new Select().contains(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__contains"), eq(value));
    }

    @Test
    void startswith() {
        final String fieldName = "field";
        final String value = "value";
        final Select select = new Select().startsWith(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__startswith"), eq(value));
    }

    @Test
    void endswith() {
        final String fieldName = "field";
        final String value = "value";
        final Select select = new Select().endsWith(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__endswith"), eq(value));
    }

    @Test
    void icontains() {
        final String fieldName = "field";
        final String value = "value";
        final Select select = new Select().containsCaseInsensitive(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__icontains"), eq(value));
    }

    @Test
    void istartswith() {
        final String fieldName = "field";
        final String value = "value";
        final Select select = new Select().startsWithCaseInsensitive(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__istartswith"), eq(value));
    }

    @Test
    void iendswith() {
        final String fieldName = "field";
        final String value = "value";
        final Select select = new Select().endsWithCaseInsensitive(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__iendswith"), eq(value));
    }

    @Test
    void gt() {
        final String fieldName = "field";
        final long value = Long.MAX_VALUE;
        final Select select = new Select().greaterThan(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__gt"), eq(String.valueOf(value)));
    }

    @Test
    void gte() {
        final String fieldName = "field";
        final long value = Long.MAX_VALUE;
        final Select select = new Select().greaterThanOrEquals(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__gte"), eq(String.valueOf(value)));
    }

    @Test
    void gteornull() {
        final String fieldName = "field";
        final long value = Long.MAX_VALUE;
        final Select select = new Select().greaterThanOrNull(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__gteornull"), eq(String.valueOf(value)));
    }

    @Test
    void lt() {
        final String fieldName = "field";
        final long value = Long.MAX_VALUE;
        final Select select = new Select().lessThan(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__lt"), eq(String.valueOf(value)));
    }

    @Test
    void lte() {
        final String fieldName = "field";
        final long value = Long.MAX_VALUE;
        final Select select = new Select().lessThanOrEquals(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__lte"), eq(String.valueOf(value)));
    }

    @Test
    void lteornull() {
        final String fieldName = "field";
        final long value = Long.MAX_VALUE;
        final Select select = new Select().lessThanOrNull(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__lteornull"), eq(String.valueOf(value)));
    }

    @Test
    void containsall() {
        final String fieldName = "field";
        final long value = Long.MAX_VALUE;
        final Select select = new Select().containsAll(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__containsall"), eq("[\"" + value + "\"]"));
    }

    @Test
    void containsany() {
        final String fieldName = "field";
        final long value = Long.MAX_VALUE;
        final Select select = new Select().containsAny(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__containsany"), eq("[\"" + value + "\"]"));
    }

    @Test
    void containsallString() {
        final String fieldName = "field";
        final String value = "value";
        final Select select = new Select().containsAll(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__containsall"), eq("[\"" + value + "\"]"));
    }

    @Test
    void containsanyString() {
        final String fieldName = "field";
        final String value = "value";
        final Select select = new Select().containsAny(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__containsany"), eq("[\"" + value + "\"]"));
    }

    @Test
    void in() {
        final String fieldName = "field";
        final long value = Long.MAX_VALUE;
        final Select select = new Select().in(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__in"), eq("[\"" + value + "\"]"));
    }

    @Test
    void inString() {
        final String fieldName = "field";
        final String value = "value";
        final Select select = new Select().in(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__in"), eq("[\"" + value + "\"]"));
    }

    @Test
    void equals() {
        final String fieldName = "field";
        final String value = "value";
        final Select select = new Select().equals(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__eq"), eq(value));
    }

    @Test
    void noteq() {
        final String fieldName = "field";
        final String value = "value";
        final Select select = new Select().notEquals(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__noteq"), eq(value));
    }
}
