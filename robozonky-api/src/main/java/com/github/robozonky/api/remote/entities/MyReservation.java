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

package com.github.robozonky.api.remote.entities;

import com.github.robozonky.api.Money;
import io.vavr.Lazy;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.time.OffsetDateTime;

public class MyReservation extends BaseEntity {

    private long id;
    private OffsetDateTime timeCreated;
    private OffsetDateTime deadline;

    @XmlElement
    private String reservedAmount = "0";
    private final Lazy<Money> moneyReservedAmount = Lazy.of(() -> Money.from(reservedAmount));

    MyReservation() {
        // for JAXB
    }

    @XmlElement
    public long getId() {
        return id;
    }

    @XmlElement
    public OffsetDateTime getTimeCreated() {
        return timeCreated;
    }

    @XmlElement
    public OffsetDateTime getDeadline() {
        return deadline;
    }

    @XmlTransient
    public Money getReservedAmount() {
        return moneyReservedAmount.get();
    }
}
