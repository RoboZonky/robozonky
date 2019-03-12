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

package com.github.robozonky.strategy.natural;

import java.math.BigDecimal;
import java.util.Objects;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.Descriptor;
import com.github.robozonky.internal.util.BigDecimalCalculator;

abstract class AbstractWrapper<T extends Descriptor<?, ?, ?>> implements Wrapper<T> {

    private final T original;

    protected AbstractWrapper(final T original) {
        this.original = original;
    }

    /**
     * Rates are coming from the API ({@link Investment}, {@link Loan}, {@link Participation}) in the form of a share
     * (1 % becomes "0.01"), while the strategy excepts them in the format of a percentage (1 & becomes "1"). This
     * method converts from the former to the latter.
     * @param bigDecimal
     * @return
     */
    protected static BigDecimal adjustRateForStrategy(final BigDecimal bigDecimal) {
        return BigDecimalCalculator.times(bigDecimal, 100);
    }

    @Override
    public T getOriginal() {
        return original;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final AbstractWrapper<T> that = (AbstractWrapper<T>) o;
        return Objects.equals(original, that.original);
    }

    @Override
    public int hashCode() {
        return Objects.hash(original);
    }
}
