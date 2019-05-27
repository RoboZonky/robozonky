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

package com.github.robozonky.api.remote.entities;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.internal.Defaults;

import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

public class DateDescriptor extends BaseEntity {

    private static final DateTimeFormatter YEAR_MONTH = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2)
            .toFormatter();
    private String date;
    private String format;

    public static OffsetDateTime toOffsetDateTime(final DateDescriptor descriptor) {
        return toOffsetDateTime(descriptor.getFormat(), descriptor.getDate());
    }

    public static OffsetDateTime toOffsetDateTime(final String format, final String date) {
        switch (format) { // the only two formats, as confirmed via e-mail with Zonky employees
            case "yyyy-MM":
                return YearMonth.parse(date, YEAR_MONTH)
                        .atDay(1)
                        .atStartOfDay(Defaults.ZONE_ID).toOffsetDateTime();
            case "yyyy-MM-dd'T'HH:mm:ssZ":
                return OffsetDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
            default:
                throw new IllegalArgumentException("Unknown date format ID: " + format);
        }
    }

    @XmlElement
    public String getDate() {
        return date;
    }

    @XmlElement
    public String getFormat() {
        return format;
    }
}
