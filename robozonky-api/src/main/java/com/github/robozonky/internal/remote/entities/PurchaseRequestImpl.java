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

import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.PurchaseRequest;

public class PurchaseRequestImpl extends BaseEntity implements PurchaseRequest {

    private BigDecimal amount;

    public PurchaseRequestImpl(final Participation participation) {
        this.amount = participation.getRemainingPrincipal()
            .getValue();
    }

    @Override
    @XmlElement
    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PurchaseRequestImpl.class.getSimpleName() + "[", "]")
            .add("amount=" + amount)
            .toString();
    }
}
