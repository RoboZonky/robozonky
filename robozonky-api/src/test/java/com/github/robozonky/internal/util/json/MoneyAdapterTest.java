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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;

public class MoneyAdapterTest {

    private final MoneyAdapter adapter = new MoneyAdapter();

    @Test
    void marshalAndUnmarshal() {
        String json = adapter.adaptToJson(Money.from("123.456"));
        Money money = adapter.adaptFromJson(json);
        Assertions.assertThat(money)
            .isEqualTo(Money.from("123.456"));
    }

    @Test
    void marshalAndUnmarshalZero() {
        String json = adapter.adaptToJson(Money.ZERO);
        Money money = adapter.adaptFromJson(json);
        Assertions.assertThat(money)
            .isSameAs(Money.ZERO);
    }

}
