/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.confirmations;

import java.util.Objects;
import java.util.OptionalInt;

import com.github.triceo.robozonky.api.Defaults;

/**
 * Response from the {@link ConfirmationProvider}.
 */
public final class Confirmation {

    private final ConfirmationType type;
    private final int amount;

    public Confirmation(final int amount) {
        if (amount < Defaults.MINIMUM_INVESTMENT_IN_CZK) {
            throw new IllegalArgumentException("Confirmed amount may not be lower than Zonky minimum.");
        }
        this.type = ConfirmationType.APPROVED;
        this.amount = amount;
    }

    public Confirmation(final ConfirmationType type) {
        this.type = type;
        this.amount = -1;
    }

    /**
     * @return The type of confirmation sent by the provider.
     */
    public ConfirmationType getType() {
        return type;
    }

    /**
     * Size of the investment confirmed by the provider.
     * @return Present only if {@link #getType()} is {@link ConfirmationType#APPROVED}.
     */
    public OptionalInt getAmount() {
        return amount < Defaults.MINIMUM_INVESTMENT_IN_CZK ? OptionalInt.empty() : OptionalInt.of(amount);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Confirmation)) {
            return false;
        }
        final Confirmation that = (Confirmation) o;
        return amount == that.amount && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, amount);
    }

    @Override
    public String toString() {
        return "Confirmation{" +
                "type=" + type +
                ", amount=" + this.getAmount() +
                '}';
    }
}
