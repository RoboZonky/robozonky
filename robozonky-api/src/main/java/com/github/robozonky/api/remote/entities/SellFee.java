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

package com.github.robozonky.api.remote.entities;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;

public class SellFee extends BaseEntity {

    @XmlElement
    private OffsetDateTime expiresAt;

    // String to be represented as money.
    @XmlElement
    private String value;

    SellFee() {
        // for JAXB
    }

    public Optional<OffsetDateTime> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    @XmlTransient
    public Money getValue() {
        return Money.from(value);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SellFee.class.getSimpleName() + "[", "]")
                .add("value='" + value + "'")
                .add("expiresAt=" + expiresAt)
                .toString();
    }
}
