/*
 * Copyright 2017 The RoboZonky Project
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

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

abstract class AbstractBaseEnumTest {

    private final Supplier<BaseEnum[]> valueSupplier;
    private final Function<String, BaseEnum> valueConverter;

    protected AbstractBaseEnumTest(final Supplier<BaseEnum[]> valueSupplier,
                                   final Function<String, BaseEnum> valueConverter) {
        this.valueSupplier = valueSupplier;
        this.valueConverter = valueConverter;
    }

    @Test
    public void testBidirectionality() {
        SoftAssertions.assertSoftly(softly -> Stream.of(valueSupplier.get()).forEach(i -> {
            final String code = i.getCode();
            final BaseEnum type = valueConverter.apply(code);
            softly.assertThat(type).isSameAs(i);
        }));
    }

    @Test
    public void testWrongCode() {
        Assertions.assertThatThrownBy(() -> valueConverter.apply(UUID.randomUUID().toString()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
