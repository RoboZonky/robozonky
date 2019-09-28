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

package com.github.robozonky.api;

import com.github.robozonky.internal.Defaults;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class MoneyTest {

    @Test
    void fromStringRoundedDownToZero() {
        Money money = Money.from("0.001");
        assertSoftly(softly -> {
            softly.assertThat(money.getCurrency()).isEqualTo(Defaults.CURRENCY);
            softly.assertThat(money.getValue()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(money.isZero()).isTrue();
            softly.assertThat(money.getZero()).isSameAs(money);
        });
    }

    @Test
    void fromString() {
        Money money = Money.from("0.005");
        assertSoftly(softly -> {
            softly.assertThat(money.getValue()).isEqualTo(new BigDecimal("0.01"));
            softly.assertThat(money.getCurrency()).isEqualTo(Defaults.CURRENCY);
            softly.assertThat(money.isZero()).isFalse();
            softly.assertThat(money.getZero()).isNotEqualTo(money);
        });
    }

    @Test
    void fromDouble() {
        Money money = Money.from(0.005);
        assertSoftly(softly -> {
            softly.assertThat(money.getValue()).isEqualTo(new BigDecimal("0.01"));
            softly.assertThat(money.getCurrency()).isEqualTo(Defaults.CURRENCY);
            softly.assertThat(money.isZero()).isFalse();
            softly.assertThat(money.getZero()).isNotEqualTo(money);
        });
    }

    @Test
    void cachesZeros() {
        Money defaultZero = Money.from(0, Defaults.CURRENCY);
        Money defaultZero2 = defaultZero.getZero();
        Money defaultZero3 = Money.getZero(Defaults.CURRENCY);
        assertSoftly(softly -> {
            softly.assertThat(defaultZero2).isSameAs(defaultZero);
            softly.assertThat(defaultZero3).isSameAs(defaultZero);
        });
    }

    @Test
    void equals() {
        Money first = Money.from(1, Defaults.CURRENCY);
        Money second = Money.from(1, Defaults.CURRENCY);
        Money third = Money.from(2, Defaults.CURRENCY);
        assertSoftly(softly -> {
            softly.assertThat(first).isEqualTo(first);
            softly.assertThat(first).isEqualTo(second);
            softly.assertThat(first).isNotEqualTo(third);
            softly.assertThat(first).isNotEqualTo(null);
            softly.assertThat(first).isNotEqualTo(UUID.randomUUID().toString());
        });
    }

    @Test
    void equalsDifferentCurrencies() {
        Money first = Money.from(0, Currency.getInstance("CZK"));
        Money second = Money.from(1, Currency.getInstance("USD"));
        Money third = Money.from(0, Currency.getInstance("USD"));
        assertSoftly(softly -> {
            softly.assertThat(first).isNotEqualTo(second);
            softly.assertThat(first).isEqualTo(third);
        });
    }

    @Test
    void comparingWithSameCurrency() {
        Money first = Money.from(0);
        Money second = Money.from(1);
        Money third = Money.from(2);
        final SortedSet<Money> money = new TreeSet<>();
        money.add(first);
        money.add(third);
        money.add(second);
        money.add(first);
        assertSoftly(softly -> {
            softly.assertThat(money).hasSize(3);
            softly.assertThat(money).first().isSameAs(first);
            softly.assertThat(money).last().isSameAs(third);
        });
    }

    @Test
    void comparingWithDifferentCurrencies() {
        Money first = Money.from(0, Currency.getInstance("CZK"));
        Money second = Money.from(0, Currency.getInstance("USD"));
        Money third = Money.from(1, Currency.getInstance("USD"));
        assertSoftly(softly -> {
            softly.assertThat(first).isEqualByComparingTo(second);
            softly.assertThatThrownBy(() -> first.compareTo(third)).isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void min() {
        Money smaller = Money.from(0);
        Money larger = Money.from(1);
        assertSoftly(softly -> {
            softly.assertThat(smaller.min(larger)).isSameAs(smaller);
            softly.assertThat(smaller.min(smaller)).isSameAs(smaller);
            softly.assertThat(larger.min(smaller)).isSameAs(smaller);
        });
    }

    @Test
    void max() {
        Money smaller = Money.from(0);
        Money larger = Money.from(1);
        assertSoftly(softly -> {
            softly.assertThat(smaller.max(larger)).isSameAs(larger);
            softly.assertThat(smaller.max(smaller)).isSameAs(smaller);
            softly.assertThat(larger.max(smaller)).isSameAs(larger);
        });
    }

    @Test
    void mathLong() {
        Money result = Money.from(0)
                .add(2)
                .subtract(1)
                .multiplyBy(10)
                .divideBy(3);
        assertThat(result).isEqualTo(Money.from(3.33));
    }

    @Test
    void mathDouble() {
        Money result = Money.from(1.4)
                .add(2.6)
                .subtract(1.5)
                .multiplyBy(2)
                .divideBy(4);
        assertThat(result).isEqualTo(Money.from(1.25));
    }

    @Test
    void mathBigDecimal() {
        Money result = Money.from(BigDecimal.ONE)
                .add(BigDecimal.TEN)
                .subtract(BigDecimal.ONE)
                .multiplyBy(BigDecimal.TEN)
                .divideBy(BigDecimal.TEN);
        assertThat(result).isEqualTo(Money.from(BigDecimal.TEN));
    }

}
