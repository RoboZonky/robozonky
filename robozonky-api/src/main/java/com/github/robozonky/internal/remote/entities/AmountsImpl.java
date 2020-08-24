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

package com.github.robozonky.internal.remote.entities;

import java.util.Objects;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Amounts;

public final class AmountsImpl implements Amounts {

    private Money total;
    private Money unpaid;

    public AmountsImpl() {
        // For JSON-B.
    }

    public AmountsImpl(final Money total) {
        this(total, total);
    }

    public AmountsImpl(final Money total, final Money unpaid) {
        this.total = Objects.requireNonNull(total);
        this.unpaid = Objects.requireNonNull(unpaid);
    }

    @Override
    public Money getTotal() {
        return Objects.requireNonNull(total);
    }

    public void setTotal(final Money total) {
        this.total = total;
    }

    @Override
    public Money getUnpaid() {
        return Objects.requireNonNull(unpaid);
    }

    public void setUnpaid(final Money unpaid) {
        this.unpaid = unpaid;
    }

    @Override
    public String toString() {
        return unpaid + " unpaid out of " + total;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final AmountsImpl amounts = (AmountsImpl) o;
        return Objects.equals(total, amounts.total) &&
                Objects.equals(unpaid, amounts.unpaid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, unpaid);
    }
}
