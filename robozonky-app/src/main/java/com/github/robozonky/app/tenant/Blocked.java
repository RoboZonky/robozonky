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

package com.github.robozonky.app.tenant;

import java.math.BigDecimal;
import java.util.Objects;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.enums.Rating;

final class Blocked {

    private final int id;
    private final BigDecimal amount;
    private final Rating rating;
    private final boolean persistent;

    public Blocked(final BigDecimal amount, final Rating rating) {
        this(amount, rating, true);
    }

    public Blocked(final BlockedAmount amount, final Rating rating) {
        this(amount, rating, true);
    }

    public Blocked(final BigDecimal amount, final Rating rating, final boolean persistent) {
        this.id = -1;
        this.amount = amount.abs();
        this.rating = rating;
        this.persistent = persistent;
    }

    public Blocked(final BlockedAmount amount, final Rating rating, final boolean persistent) {
        this.id = amount.getLoanId();
        this.amount = amount.getAmount().abs();
        this.rating = rating;
        this.persistent = persistent;
    }

    public int getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Rating getRating() {
        return rating;
    }

    /**
     *
     * @return True if this information should continue to be persisted after the next remote read of blocked amounts.
     */
    public boolean isPersistent() {
        return persistent;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final Blocked blocked = (Blocked) o;
        return id == blocked.id &&
                Objects.equals(amount, blocked.amount) &&
                rating == blocked.rating;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, amount, rating);
    }

    @Override
    public String toString() {
        return "Blocked{" +
                "amount=" + amount +
                ", persistent=" + persistent +
                ", id=" + id +
                ", rating=" + rating +
                '}';
    }
}
