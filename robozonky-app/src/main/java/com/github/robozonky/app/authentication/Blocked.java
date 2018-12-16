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

package com.github.robozonky.app.authentication;

import java.math.BigDecimal;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.enums.Rating;

final class Blocked {

    private final int id;
    private final BigDecimal amount;
    private final Rating rating;

    public Blocked(final BigDecimal amount, final Rating rating) {
        this.id = -1;
        this.amount = amount.abs();
        this.rating = rating;
    }

    public Blocked(final BlockedAmount amount, final Rating rating) {
        this.id = amount.getId();
        this.amount = amount.getAmount().abs();
        this.rating = rating;
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
}
