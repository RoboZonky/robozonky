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

package com.github.robozonky.api.remote.entities;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlElement;

public class OverallPortfolio extends BaseEntity {

    private BigDecimal unpaid, paid, due;

    OverallPortfolio() {
        // for JAXB
    }

    public OverallPortfolio(final long paid, final long unpaid, final long due) {
        this.unpaid = BigDecimal.valueOf(unpaid);
        this.paid = BigDecimal.valueOf(paid);
        this.due = BigDecimal.valueOf(due);
    }

    @XmlElement
    public BigDecimal getUnpaid() {
        return unpaid;
    }

    @XmlElement
    public BigDecimal getPaid() {
        return paid;
    }

    @XmlElement
    public BigDecimal getDue() {
        return due;
    }
}
