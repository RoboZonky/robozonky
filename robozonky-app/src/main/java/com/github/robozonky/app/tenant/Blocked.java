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

package com.github.robozonky.app.tenant;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.test.DateUtil;

final class Blocked {

    private final int id;
    private final BigDecimal amount;
    private final Rating rating;
    private final boolean persistent;
    private final OffsetDateTime storedOn = DateUtil.offsetNow();

    Blocked(final int id, final BigDecimal amount, final Rating rating) {
        this(id, amount, rating, false);
    }

    public Blocked(final int id, final BigDecimal amount, final Rating rating, final boolean persistent) {
        this.id = id;
        this.amount = amount.abs();
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

    public boolean isValidInStatistics(final RemoteData remoteData) {
        return storedOn.isAfter(remoteData.getStatistics().getTimestamp());
    }

    public boolean isValidInBalance(final RemoteData remoteData) {
        return storedOn.isAfter(remoteData.getRetrievedOn());
    }

    public boolean isValid(final RemoteData remoteData) {
        return persistent || isValidInStatistics(remoteData) || isValidInBalance(remoteData);
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
        return "BlockedAmount{" +
                "amount=" + amount +
                ", id=" + id +
                ", persistent=" + persistent +
                ", rating=" + rating +
                ", storedOn=" + storedOn +
                '}';
    }
}
