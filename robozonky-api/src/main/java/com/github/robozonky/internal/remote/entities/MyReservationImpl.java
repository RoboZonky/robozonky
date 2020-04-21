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

import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.MyReservation;

public class MyReservationImpl extends BaseEntity implements MyReservation {

    private long id;

    @XmlElement
    private String reservedAmount = "0";

    /*
     * Don't waste time deserializing some types, as we're never going to use them. Yet we do not want these reported as
     * unknown fields by Jackson.
     */
    @XmlElement
    private Object timeCreated;
    @XmlElement
    private Object deadline;

    MyReservationImpl() {
        // for JAXB
    }

    @Override
    @XmlElement
    public long getId() {
        return id;
    }

    @Override
    @XmlTransient
    public Money getReservedAmount() {
        return Money.from(reservedAmount);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MyReservationImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("reservedAmount='" + reservedAmount + "'")
            .toString();
    }
}
