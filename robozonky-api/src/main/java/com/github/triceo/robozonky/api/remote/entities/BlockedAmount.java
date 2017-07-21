/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.remote.entities;

import java.time.OffsetDateTime;
import javax.xml.bind.annotation.XmlElement;

public class BlockedAmount extends BaseEntity {

    private int amount, loanId;
    private String category, loanName;
    private OffsetDateTime dateStart;

    public BlockedAmount(final int loanId, final int loanAmount) {
        this.loanId = loanId;
        this.amount = loanAmount;
    }

    private BlockedAmount() {
        // for JAXB
    }

    @XmlElement
    public int getAmount() {
        return amount;
    }

    @XmlElement
    public int getLoanId() {
        return loanId;
    }

    @XmlElement
    public String getCategory() {
        return category;
    }

    @XmlElement
    public String getLoanName() {
        return loanName;
    }

    @XmlElement
    public OffsetDateTime getDateStart() {
        return dateStart;
    }
}
