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

package com.github.robozonky.common.remote;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
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
    void ltDate() {
        final String fieldName = "field";
        final LocalDate value = LocalDate.of(1, 2, 3);
        final Select select = new Select().lessThan(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__lt"), eq("0001-02-03"));
    }

    @Test
    void lteDate() {
        final String fieldName = "field";
        final LocalDate value = LocalDate.of(1, 2, 3);
        final Select select = new Select().lessThanOrEquals(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__lte"), eq("0001-02-03"));
    }

    @Test
    void gtDate() {
        final String fieldName = "field";
        final LocalDate value = LocalDate.of(1, 2, 3);
        final Select select = new Select().greaterThan(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__gt"), eq("0001-02-03"));
    }

    @Test
    void gteDate() {
        final String fieldName = "field";
        final LocalDate value = LocalDate.of(1, 2, 3);
        final Select select = new Select().greaterThanOrEquals(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__gte"), eq("0001-02-03"));
    }

    @Test
    void ltDateTime() {
        final String fieldName = "field";
        final LocalDate date = LocalDate.of(2000, 01, 02);
        final LocalTime time = LocalTime.of(4, 5, 6);
        final OffsetDateTime value = LocalDateTime.of(date, time).atZone(Defaults.ZONE_ID).toOffsetDateTime();
        final Select select = new Select().lessThan(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__lt"), eq("2000-01-02T04:05:06+01:00"));
    }

    @Test
    void lteDateTime() {
        final String fieldName = "field";
        final LocalDate date = LocalDate.of(2000, 01, 02);
        final LocalTime time = LocalTime.of(4, 5, 6);
        final OffsetDateTime value = LocalDateTime.of(date, time).atZone(Defaults.ZONE_ID).toOffsetDateTime();
        final Select select = new Select().lessThanOrEquals(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__lte"), eq("2000-01-02T04:05:06+01:00"));
    }

    @Test
    void gtDateTime() {
        final String fieldName = "field";
        final LocalDate date = LocalDate.of(2000, 01, 02);
        final LocalTime time = LocalTime.of(4, 5, 6);
        final OffsetDateTime value = LocalDateTime.of(date, time).atZone(Defaults.ZONE_ID).toOffsetDateTime();
        final Select select = new Select().greaterThan(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__gt"), eq("2000-01-02T04:05:06+01:00"));
    }

    @Test
    void gteDateTime() {
        final String fieldName = "field";
        final LocalDate date = LocalDate.of(2000, 01, 02);
        final LocalTime time = LocalTime.of(4, 5, 6);
        final OffsetDateTime value = LocalDateTime.of(date, time).atZone(Defaults.ZONE_ID).toOffsetDateTime();
        final Select select = new Select().greaterThanOrEquals(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName + "__gte"), eq("2000-01-02T04:05:06+01:00"));
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
    void equality() {
        final Select select = Select.unrestricted();
        assertThat(select).isEqualTo(select);
        assertThat(select).isNotEqualTo(null);
        final Select select2 = Select.unrestricted().greaterThan("a", 1);
        assertThat(select2).isNotEqualTo(select);
        assertThat(select).isNotEqualTo(select2);
        final Select select3 = Select.unrestricted().greaterThan("a", 1);
        assertThat(select2).isEqualTo(select3);
        assertThat(select3).isEqualTo(select2);
        final Select select4 = Select.unrestricted().greaterThan("a", 2);
        assertThat(select4).isNotEqualTo(select3);
        assertThat(select3).isNotEqualTo(select4);
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
    void equalsPlain() {
        final String fieldName = UUID.randomUUID().toString();
        final String value = "value";
        final Select select = Select.unrestricted().equalsPlain(fieldName, value);
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        select.accept(filter);
        verify(filter).setQueryParam(eq(fieldName), eq(value));
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
