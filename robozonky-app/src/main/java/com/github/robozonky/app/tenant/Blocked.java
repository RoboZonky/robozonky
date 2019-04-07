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
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.util.DateUtil;

final class Blocked {

    private final int id;
    private final long storedOn = DateUtil.now().toEpochMilli();
    private final BigDecimal amount;
    private final Rating rating;
    private final boolean persistent;

    Blocked(final int id, final BigDecimal amount, final Rating rating) {
        this(id, amount, rating, false);
    }

    Blocked(final BlockedAmount amount, final Rating rating) {
        this(amount, rating, false);
    }

    public Blocked(final int id, final BigDecimal amount, final Rating rating, final boolean persistent) {
        this.id = id;
        this.amount = amount.abs();
        this.rating = rating;
        this.persistent = persistent;
    }

    Blocked(final BlockedAmount amount, final Rating rating, final boolean persistent) {
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
     * Zonky updates portfolio every 2 hours, but participations become part of the portfolio immediately, without any
     * relevant blocked amount. Therefore, this method decides whether a particular blocked amount was already included
     * in the portfolio by a given time.
     * @param latestPortfolio The latest portfolio to compare against.
     * @return True if the blocked amount had shown before the update, or if the blocked amount was set as persistent,
     * false otherwise.
     */
    public boolean isUnreflected(final Statistics latestPortfolio) {
        if (persistent) {
            return true;
        }
        final long lastUpdate = latestPortfolio.getTimestamp().toInstant().toEpochMilli();
        return lastUpdate < storedOn;
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
                "id=" + id +
                ", amount=" + amount +
                ", rating=" + rating +
                ", storedOn=" + storedOn +
                ", persistent=" + persistent +
                '}';
    }
}
