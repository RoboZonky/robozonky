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

import java.math.BigDecimal;
import java.util.StringJoiner;

import com.github.robozonky.api.remote.entities.Participation;

public class PurchaseRequest {

    private BigDecimal amount;
    private BigDecimal discount;

    public PurchaseRequest(final Participation participation) {
        this.amount = participation.getRemainingPrincipal()
            .getValue();
        this.discount = participation.getDiscount()
            .getValue();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(final BigDecimal discount) {
        this.discount = discount;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PurchaseRequest.class.getSimpleName() + "[", "]")
            .add("amount=" + amount)
            .add("discount=" + discount)
            .toString();
    }
}
