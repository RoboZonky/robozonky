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
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Currency;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.github.robozonky.internal.util.BigDecimalCalculator.*;
import static java.math.BigDecimal.valueOf;

/**
 * Represents a monetary amount of any size, in any currency. Rounds to 2 decimal points. Two instances equal if their
 * {@link #getCurrency()} and {@link #getValue()} equal, or when {@link #isZero()} regardless of currency.
 */
public final class Money implements Comparable<Money> {

    private static final Map<Currency, Money> ZEROS = new ConcurrentHashMap<>(1);
    /**
     * Do not use this. Rather, get one with the proper currency using {@link #getZero()} or {@link #getZero(Currency)}.
     */
    public static final Money ZERO = getZero(Defaults.CURRENCY);
    private final BigDecimal value;
    private final Currency currency;

    private Money(final BigDecimal value, final Currency currency) {
        this.value = value;
        this.currency = currency;
    }

    private static BigDecimal trim(final BigDecimal number) {
        return number.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money sum(Collection<Money> money) {
        return sum(money.stream());
    }

    public static Money sum(Stream<Money> money) {
        return money.reduce(ZERO, Money::add);
    }

    public static Money from(final String number) {
        return from(number, Defaults.CURRENCY);
    }

    public static Money from(final String number, final Currency currency) {
        Objects.requireNonNull(number);
        Objects.requireNonNull(currency);
        return from(new BigDecimal(number), currency);
    }

    public static Money from(final BigDecimal number) {
        return from(number, Defaults.CURRENCY);
    }

    public static Money from(final BigDecimal number, final Currency currency) {
        Objects.requireNonNull(number);
        Objects.requireNonNull(currency);
        final BigDecimal trimmed = trim(number);
        if (trimmed.signum() == 0) {
            return getZero(currency);
        }
        return new Money(trimmed, currency);
    }

    public static Money from(final long number) {
        return from(number, Defaults.CURRENCY);
    }

    public static Money from(final long number, final Currency currency) {
        Objects.requireNonNull(currency);
        return from(valueOf(number), currency);
    }

    public static Money from(final double number) {
        return from(number, Defaults.CURRENCY);
    }

    public static Money from(final double number, final Currency currency) {
        Objects.requireNonNull(currency);
        return from(valueOf(number), currency);
    }

    public static Money getZero(final Currency currency) {
        return ZEROS.computeIfAbsent(currency, key -> new Money(BigDecimal.ZERO, key));
    }

    public BigDecimal getValue() {
        return value;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Money add(final BigDecimal amount) {
        return add(from(amount, currency));
    }

    public Money add(final long amount) {
        return add(from(amount, currency));
    }

    public Money add(final double amount) {
        return add(from(amount, currency));
    }

    public Money add(final Money money) {
        return from(plus(value, money.value), currency);
    }

    public Money subtract(final BigDecimal amount) {
        return subtract(from(amount, currency));
    }

    public Money subtract(final long amount) {
        return subtract(from(amount, currency));
    }

    public Money subtract(final double amount) {
        return subtract(from(amount, currency));
    }

    public Money subtract(final Money money) {
        return from(minus(value, money.value), currency);
    }

    public Money multiplyBy(final BigDecimal amount) {
        return multiplyBy(from(amount, currency));
    }

    public Money multiplyBy(final long amount) {
        return multiplyBy(from(amount, currency));
    }

    public Money multiplyBy(final double amount) {
        return multiplyBy(from(amount, currency));
    }

    public Money multiplyBy(final Money money) {
        return from(times(value, money.value), currency);
    }

    public Money divideBy(final BigDecimal amount) {
        return divideBy(from(amount, currency));
    }

    public Money divideBy(final long amount) {
        return divideBy(from(amount, currency));
    }

    public Money divideBy(final double amount) {
        return divideBy(from(amount, currency));
    }

    public Money divideBy(final Money money) {
        return from(divide(value, money.value), currency);
    }

    public Money min(final Money money) {
        return compareTo(money) <= 0 ? this : money;
    }

    public Money max(final Money money) {
        return compareTo(money) >= 0 ? this : money;
    }

    public boolean isZero() {
        return value.signum() == 0;
    }

    public Money getZero() {
        return getZero(currency);
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
        if (isZero() && money.isZero()) { // currency doesn't matter
            return true;
        }
        return Objects.equals(value, money.value) &&
                Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, currency);
    }

    /**
     * @param o
     * @return
     * @throws IllegalArgumentException When the two non-zero instances don't share {@link #getCurrency()}.
     */
    @Override
    public int compareTo(Money o) {
        if (Objects.equals(this, o)) {
            return 0;
        } else if (!Objects.equals(currency, o.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies: " + this + " and " + o);
        } else {
            return value.compareTo(o.value);
        }

    }
}
