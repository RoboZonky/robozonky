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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Currency;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.github.robozonky.internal.util.BigDecimalCalculator.*;
import static java.math.BigDecimal.valueOf;

public final class Money implements Comparable<Money> {

    private static final Map<Currency, Money> ZEROS = new ConcurrentHashMap<>(1);
    /**
     * Do not use this. Rather, get one with the proper currency using {@link #getZero()} or {@link #getZero(Currency)}.
     */
    public static final Money ZERO = getZero(Defaults.CURRENCY);
    private final BigDecimal value;
    private final Currency currency;

    private Money(final BigDecimal value, final Currency currency) {
        this.value = value.stripTrailingZeros();
        this.currency = currency;
    }

    public static Money sum(Collection<Money> money) {
        return sum(money.stream());
    }

    public static Money sum(Stream<Money> money) {
        return money.reduce(Money.ZERO, Money::add);
    }

    public static Money from(final String number) {
        return from(number, Defaults.CURRENCY);
    }

    public static Money from(final String number, final Currency currency) {
        Objects.requireNonNull(number);
        Objects.requireNonNull(currency);
        return new Money(new BigDecimal(number), currency);
    }

    public static Money from(final BigDecimal number) {
        return from(number, Defaults.CURRENCY);
    }

    public static Money from(final BigDecimal number, final Currency currency) {
        Objects.requireNonNull(number);
        Objects.requireNonNull(currency);
        return new Money(number, currency);
    }

    public static Money from(final int number) {
        return from((long) number);
    }

    public static Money from(final int number, final Currency currency) {
        return from((long) number, currency);
    }

    public static Money from(final long number) {
        return from(number, Defaults.CURRENCY);
    }

    public static Money from(final long number, final Currency currency) {
        Objects.requireNonNull(currency);
        return new Money(valueOf(number), currency);
    }

    public static Money from(final float number) {
        return from((double) number);
    }

    public static Money from(final float number, final Currency currency) {
        return from((double) number, currency);
    }

    public static Money from(final double number) {
        return from(number, Defaults.CURRENCY);
    }

    public static Money from(final double number, final Currency currency) {
        Objects.requireNonNull(currency);
        return new Money(valueOf(number), currency);
    }

    public static Money getZero(final Currency currency) {
        return ZEROS.computeIfAbsent(currency, key -> new Money(BigDecimal.ZERO, key));
    }

    private static int compare(Money money, Money money2) {
        if (money.isZero() && money2.isZero()) { // currency doesn't matter
            return 0;
        } else if (!Objects.equals(money.currency, money2.currency)) {
            throw new IllegalStateException("Cannot compare different currencies: " + money + " and " + money2);
        } else {
            return money.value.compareTo(money2.value);
        }

    }

    public BigDecimal getValue() {
        return value;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Money add(final int amount) {
        return add(Money.from(amount, getCurrency()));
    }

    public Money add(final long amount) {
        return add(Money.from(amount, getCurrency()));
    }

    public Money add(final double amount) {
        return add(Money.from(amount, getCurrency()));
    }

    public Money add(final Money money) {
        return new Money(plus(value, money.value), currency);
    }

    public Money subtract(final int amount) {
        return subtract(Money.from(amount, getCurrency()));
    }

    public Money subtract(final long amount) {
        return subtract(Money.from(amount, getCurrency()));
    }

    public Money subtract(final double amount) {
        return subtract(Money.from(amount, getCurrency()));
    }

    public Money subtract(final Money money) {
        return new Money(minus(value, money.value), currency);
    }

    public Money multiplyBy(final int amount) {
        return multiplyBy(Money.from(amount, getCurrency()));
    }

    public Money multiplyBy(final long amount) {
        return multiplyBy(Money.from(amount, getCurrency()));
    }

    public Money multiplyBy(final double amount) {
        return multiplyBy(Money.from(amount, getCurrency()));
    }

    public Money multiplyBy(final Money money) {
        return new Money(times(value, money.value), currency);
    }

    public Money divideBy(final int amount) {
        return divideBy(Money.from(amount, getCurrency()));
    }

    public Money divideBy(final long amount) {
        return divideBy(Money.from(amount, getCurrency()));
    }

    public Money divideBy(final double amount) {
        return divideBy(Money.from(amount, getCurrency()));
    }

    public Money divideBy(final Money money) {
        return new Money(divide(value, money.value), currency);
    }

    public Money min(final Money money) {
        return this.compareTo(money) <= 0 ? this : money;
    }

    public Money max(final Money money) {
        return this.compareTo(money) >= 0 ? this : money;
    }

    public boolean isZero() {
        return getValue().signum() == 0;
    }

    public Money getZero() {
        return getZero(getCurrency());
    }

    @Override
    public String toString() {
        return value.toPlainString() + " " + currency;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final Money money = (Money) o;
        final int comparison = compare(this, money);
        return comparison == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, currency);
    }

    @Override
    public int compareTo(Money o) {
        return compare(this, o);
    }
}
