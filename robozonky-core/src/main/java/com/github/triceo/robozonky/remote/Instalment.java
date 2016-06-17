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
package com.github.triceo.robozonky.remote;

import java.math.BigDecimal;
import java.time.Instant;
import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class Instalment implements BaseEntity {

    private BigDecimal instalmentAmount, principalPaid, interestPaid;
    private Instant month;

    Instalment() {
        // for JAXB
    }

    @XmlElement
    public BigDecimal getInstalmentAmount() {
        return instalmentAmount;
    }

    @XmlElement
    public BigDecimal getPrincipalPaid() {
        return principalPaid;
    }

    @XmlElement
    public BigDecimal getInterestPaid() {
        return interestPaid;
    }

    @XmlElement
    @JsonDeserialize(using = InstantDeserializer.class)
    public Instant getMonth() {
        return month;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Instalment{");
        sb.append("instalmentAmount=").append(instalmentAmount);
        sb.append(", principalPaid=").append(principalPaid);
        sb.append(", interestPaid=").append(interestPaid);
        sb.append(", month=").append(month);
        sb.append('}');
        return sb.toString();
    }
}
