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

package com.github.robozonky.api.remote.entities.sanitized;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.DateDescriptor;
import com.github.robozonky.api.remote.entities.RawDevelopment;
import com.github.robozonky.api.remote.enums.DevelopmentType;
import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevelopmentTest {

    @Mock
    private RawDevelopment mocked;

    @Test
    @DisplayName("Sanitization works.")
    void sanitizing() {
        final DateDescriptor dd = mock(DateDescriptor.class);
        when(dd.getFormat()).thenReturn("yyyy-MM");
        when(dd.getDate()).thenReturn("2018-01");
        when(mocked.getDateFrom()).thenReturn(dd);
        final OffsetDateTime expected = YearMonth.of(2018, 1)
                .atDay(1)
                .atStartOfDay(Defaults.ZONE_ID)
                .toOffsetDateTime();
        assertThat(Development.sanitized(mocked).getDateFrom()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Custom development works.")
    void custom() {
        final Development d = Development.custom().build();
        assertThat(d).isNotNull();
    }

    @Nested
    @DisplayName("Setters for ")
    class SetterTest {

        private final DevelopmentBuilder db = Development.custom();

        private <T> void standard(final DevelopmentBuilder builder, final Function<T, DevelopmentBuilder> setter,
                                  final Supplier<T> getter, final T value) {
            assertThat(getter.get()).as("Null before setting.").isNull();
            final DevelopmentBuilder newBuilder = setter.apply(value);
            assertSoftly(softly -> {
                softly.assertThat(newBuilder).as("Setter returned itself.").isSameAs(builder);
                softly.assertThat(getter.get()).as("Correct value was set.").isEqualTo(value);
            });
        }

        private <T> void optional(final DevelopmentBuilder builder, final Function<T, DevelopmentBuilder> setter,
                                  final Supplier<Optional<T>> getter, final T value) {
            assertThat(getter.get()).isEmpty();
            final DevelopmentBuilder newBuilder = setter.apply(value);
            assertSoftly(softly -> {
                softly.assertThat(newBuilder).as("Setter returned itself.").isSameAs(builder);
                softly.assertThat(getter.get()).as("Correct value was set.").contains(value);
            });
        }

        @Test
        void publicNote() {
            optional(db, db::setPublicNote, db::getPublicNote, "something");
        }

        @Test
        void type() {
            standard(db, db::setType, db::getType, DevelopmentType.OTHER);
        }

        @Test
        void dateTo() {
            optional(db, db::setDateTo, db::getDateTo, OffsetDateTime.now());
        }

        @Test
        void dateFrom() {
            standard(db, db::setDateFrom, db::getDateFrom, OffsetDateTime.now());
        }
    }
}
